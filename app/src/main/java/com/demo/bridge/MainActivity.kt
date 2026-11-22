package com.demo.bridge

import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import rikka.shizuku.Shizuku

class MainActivity(
    private val context: Context,
    private val statusText: TextView,
    private val authButton: Button,
    private val execButton: Button,
    private val commandInput: EditText,
    private val outputText: TextView,
    private val outputScroll: ScrollView
) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val requestCode = 1001

    private val permissionListener =
        Shizuku.OnRequestPermissionResultListener { code, grantResult ->
            if (code == this.requestCode) {
                if (grantResult == PackageManager.PERMISSION_GRANTED) {
                    statusText.text = "状态: Shizuku 已授权"
                    authButton.isEnabled = false
                    execButton.isEnabled = true
                } else {
                    statusText.text = "状态: Shizuku 授权被拒绝"
                    authButton.isEnabled = true
                    execButton.isEnabled = false
                }
            }
        }

    init {
        Shizuku.addRequestPermissionResultListener(permissionListener)

        authButton.setOnClickListener {
            if (!isShizukuReady()) {
                statusText.text = "状态: Shizuku 未安装或未启动"
                return@setOnClickListener
            }
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                statusText.text = "状态: Shizuku 已授权"
                authButton.isEnabled = false
                execButton.isEnabled = true
            } else {
                Shizuku.requestPermission(requestCode)
            }
        }

        execButton.setOnClickListener {
            if (!execButton.isEnabled) return@setOnClickListener
            val cmd = commandInput.text.toString().trim()
            if (cmd.isEmpty()) {
                outputText.text = "命令不能为空"
                return@setOnClickListener
            }
            executeCommand(cmd)
        }

        checkShizukuStatus()
    }

    fun onDestroy() {
        Shizuku.removeRequestPermissionResultListener(permissionListener)
    }

    private fun isShizukuReady(): Boolean = try {
        Shizuku.pingBinder()
    } catch (e: Exception) {
        false
    }

    private fun checkShizukuStatus() {
        if (!isShizukuReady()) {
            statusText.text = "状态: Shizuku 未安装或未启动"
            authButton.isEnabled = false
            execButton.isEnabled = false
            return
        }
        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            statusText.text = "状态: Shizuku 已授权"
            authButton.isEnabled = false
            execButton.isEnabled = true
        } else {
            statusText.text = "状态: Shizuku 已安装，未授权"
            authButton.isEnabled = true
            execButton.isEnabled = false
        }
    }

    private fun executeCommand(command: String) {
        if (!isShizukuReady() ||
            Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED
        ) {
            outputText.text = "Shizuku 未就绪或未授权"
            return
        }

        execButton.isEnabled = false
        outputText.text = "执行中..."

        Thread {
            var result = ""
            var error = ""
            var exitCode = -1

            try {
                val proc = buildProcess(context, arrayOf("sh", "-c", command))
                try {
                    result = proc.inputStream.bufferedReader().use { it.readText() }
                    error = proc.errorStream.bufferedReader().use { it.readText() }
                    exitCode = proc.waitFor()
                } finally {
                    proc.destroy()
                }
            } catch (e: Throwable) {
                error = "Shizuku 执行失败: ${e.message ?: e.javaClass.simpleName}"
            }

            val out = buildString {
                if (result.isNotBlank()) append(result)
                if (error.isNotBlank()) append("\n错误:\n$error")
                append("\n退出码: $exitCode")
            }

            mainHandler.post {
                outputText.text = out.ifBlank { "执行完成，无输出" }
                outputScroll.post { outputScroll.fullScroll(ScrollView.FOCUS_DOWN) }
                execButton.isEnabled = true
            }
        }.start()
    }

    private fun buildProcess(ctx: Context, cmd: Array<String>): Process {
        try {
            val m = Shizuku::class.java.getDeclaredMethod(
                "newRemoteProcessBuilder",
                Context::class.java,
                Array<String>::class.java
            )
            m.isAccessible = true
            val builder = m.invoke(null, ctx, cmd)
            return builder.javaClass.getMethod("start").invoke(builder) as Process
        } catch (ignored: Throwable) {
        }

        try {
            val m = Shizuku::class.java.getDeclaredMethod("getRemoteProcessBuilder")
            m.isAccessible = true
            val builder = m.invoke(null)
            builder.javaClass
                .getMethod("command", Array<String>::class.java)
                .invoke(builder, cmd)
            return builder.javaClass.getMethod("start").invoke(builder) as Process
        } catch (e: Throwable) {
            throw RuntimeException("Shizuku 进程创建失败: ${e.message}", e)
        }
    }
}
