package com.demo.bridge

import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import rikka.shizuku.Shizuku

class MainActivity : Activity() {

    private lateinit var statusText: TextView
    private lateinit var authButton: Button
    private lateinit var execButton: Button
    private lateinit var commandInput: EditText
    private lateinit var outputText: TextView
    private lateinit var outputScroll: ScrollView

    private val permissionListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        if (requestCode == REQUEST_CODE_PERMISSION) {
            runOnUiThreadSafe {
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
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
        }

        statusText = TextView(this).apply {
            text = "状态: 检测中..."
            textSize = 18f
        }

        authButton = Button(this).apply {
            text = "请求 Shizuku 授权"
            isEnabled = false
        }

        commandInput = EditText(this).apply {
            setText("pm list packages")
            textSize = 16f
            setPadding(0, 24, 0, 0)
        }

        execButton = Button(this).apply {
            text = "执行命令"
            isEnabled = false
        }

        outputText = TextView(this).apply {
            text = "等待执行..."
            textSize = 14f
        }

        outputScroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
            addView(outputText)
        }

        root.addView(statusText)
        root.addView(authButton)
        root.addView(commandInput)
        root.addView(execButton)
        root.addView(outputScroll)
        setContentView(root)

        Shizuku.addRequestPermissionResultListener(permissionListener)

        authButton.setOnClickListener {
            if (!isShizukuReady()) {
                Toast.makeText(this, "Shizuku 未就绪", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                statusText.text = "状态: Shizuku 已授权"
                authButton.isEnabled = false
                execButton.isEnabled = true
            } else {
                Shizuku.requestPermission(REQUEST_CODE_PERMISSION)
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

    private fun runOnUiThreadSafe(block: () -> Unit) {
        Handler(Looper.getMainLooper()).post(block)
    }

    private fun isShizukuReady(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (e: Exception) {
            false
        }
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
                val cmdArray = arrayOf("sh", "-c", command)
                val ctx = applicationContext
                var proc: Process? = null

                try {
                    val m = Shizuku::class.java.getMethod(
                        "newRemoteProcessBuilder",
                        android.content.Context::class.java,
                        Array<String>::class.java
                    )
                    val builder = m.invoke(null, ctx, cmdArray)
                    proc = builder.javaClass.getMethod("start").invoke(builder) as Process
                } catch (t: Throwable) {
                    val m = Shizuku::class.java.getMethod("getRemoteProcessBuilder")
                    val builder = m.invoke(null)
                    builder.javaClass
                        .getMethod("command", Array<String>::class.java)
                        .invoke(builder, cmdArray)
                    proc = builder.javaClass.getMethod("start").invoke(builder) as Process
                }

                val p = proc
                if (p != null) {
                    try {
                        result = p.inputStream.bufferedReader().readText()
                        error = p.errorStream.bufferedReader().readText()
                        exitCode = p.waitFor()
                    } finally {
                        p.destroy()
                    }
                } else {
                    error = "未获取到进程"
                }
            } catch (e: Throwable) {
                error = if (error.isBlank()) {
                    "Shizuku 执行失败: ${e.message}"
                } else {
                    "$error\nShizuku 执行失败: ${e.message}"
                }
            }

            val out = buildString {
                if (result.isNotBlank()) append(result)
                if (error.isNotBlank()) append("\n错误:\n$error")
                append("\n退出码: $exitCode")
            }

            runOnUiThreadSafe {
                outputText.text = out.ifBlank { "执行完成，无输出" }
                outputScroll.post { outputScroll.fullScroll(ScrollView.FOCUS_DOWN) }
                execButton.isEnabled = true
            }
        }.start()
    }

    companion object {
        private const val REQUEST_CODE_PERMISSION = 1001
    }
}
