package com.demo.bridge

import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import rikka.shizuku.Shizuku

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var authButton: Button
    private lateinit var execButton: Button
    private lateinit var commandInput: EditText
    private lateinit var outputText: TextView
    private lateinit var outputScroll: ScrollView

    companion object {
        private const val REQUEST_CODE_PERMISSION = 1001
    }

    private val permissionListener = Shizuku.OnRequestPermissionResultListener { _, grantResult ->
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        authButton = findViewById(R.id.authButton)
        execButton = findViewById(R.id.execButton)
        commandInput = findViewById(R.id.commandInput)
        outputText = findViewById(R.id.outputText)
        outputScroll = findViewById(R.id.outputScroll)

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
                var proc: Process? = null

                try {
                    val m = Shizuku::class.java.getMethod(
                        "newRemoteProcessBuilder",
                        android.content.Context::class.java,
                        Array<String>::class.java
                    )
                    val builder = m.invoke(null, applicationContext, cmdArray)
                    proc = builder.javaClass.getMethod("start").invoke(builder) as Process
                } catch (t: Throwable) {
                    val m = Shizuku::class.java.getMethod("getRemoteProcessBuilder")
                    val builder = m.invoke(null)
                    builder.javaClass
                        .getMethod("command", Array<String>::class.java)
                        .invoke(builder, cmdArray)
                    proc = builder.javaClass.getMethod("start").invoke(builder) as Process
                }

                proc?.use { p ->
                    result = p.inputStream.bufferedReader().readText()
                    error = p.errorStream.bufferedReader().readText()
                    exitCode = p.waitFor()
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

            Handler(Looper.getMainLooper()).post {
                outputText.text = out.ifBlank { "执行完成，无输出" }
                outputScroll.post { outputScroll.fullScroll(ScrollView.FOCUS_DOWN) }
                execButton.isEnabled = true
            }
        }.start()
    }
}
