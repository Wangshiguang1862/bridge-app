package com.demo.bridge

import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import rikka.shizuku.Shizuku

class MainActivity : Activity() {

    private lateinit var statusText: TextView
    private lateinit var authButton: Button
    private lateinit var execButton: Button
    private lateinit var commandInput: TextView
    private lateinit var outputScroll: ScrollView
    private lateinit var outputText: TextView

    private val REQUEST_CODE = 1001

    private val permissionListener = object : Shizuku.OnRequestPermissionResultListener {
        override fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {
            if (grantResult == PackageManager.PERMISSION_GRANTED) {
                updateStatus("Shizuku 已授权")
                authButton.isEnabled = false
                execButton.isEnabled = true
            } else {
                updateStatus("Shizuku 授权被拒绝")
                Toast.makeText(this@MainActivity, "授权被拒绝，将使用降级模式", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 96, 48, 48)
        }

        statusText = TextView(this).apply {
            text = "正在检测 Shizuku..."
            textSize = 18f
        }

        authButton = Button(this).apply {
            text = "请求 Shizuku 授权"
            isEnabled = false
        }

        commandInput = TextView(this).apply {
            text = "pm list packages"
            textSize = 16f
            setPadding(0, 24, 0, 0)
        }

        execButton = Button(this).apply {
            text = "执行命令"
            isEnabled = false
        }

        outputScroll = ScrollView(this)
        outputText = TextView(this).apply {
            text = "等待执行..."
            textSize = 14f
            setPadding(0, 24, 0, 0)
        }
        outputScroll.addView(outputText)

        root.addView(statusText)
        root.addView(authButton)
        root.addView(commandInput)
        root.addView(execButton)
        root.addView(outputScroll)
        setContentView(root)

        Shizuku.addRequestPermissionResultListener(permissionListener)

        authButton.setOnClickListener {
            if (!Shizuku.pingBinder()) {
                updateStatus("Shizuku 未运行，请先启动 Shizuku")
                return@setOnClickListener
            }
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                updateStatus("Shizuku 已授权")
                execButton.isEnabled = true
                authButton.isEnabled = false
            } else {
                Shizuku.requestPermission(REQUEST_CODE)
            }
        }

        execButton.setOnClickListener {
            val cmd = commandInput.text.toString().trim()
            if (cmd.isEmpty()) {
                outputText.text = "命令不能为空"
                return@setOnClickListener
            }
            executeCommand(cmd)
        }

        checkShizukuStatus()
    }

    private fun checkShizukuStatus() {
        if (!Shizuku.pingBinder()) {
            updateStatus("Shizuku 未安装或未启动")
            authButton.isEnabled = false
            execButton.isEnabled = false
            return
        }
        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            updateStatus("Shizuku 已授权")
            authButton.isEnabled = false
            execButton.isEnabled = true
        } else {
            updateStatus("Shizuku 已安装，未授权")
            authButton.isEnabled = true
            execButton.isEnabled = false
        }
    }

    private fun executeCommand(command: String) {
        try {
            // 通过 Shizuku 的 shell 执行命令
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
            val result = process.inputStream.bufferedReader().readText()
            val error = process.errorStream.bufferedReader().readText()
            process.waitFor()

            val output = buildString {
                if (result.isNotBlank()) append(result)
                if (error.isNotBlank()) append("\n错误:\n$error")
            }
            outputText.text = output.ifBlank { "命令执行完成，无输出" }
        } catch (e: Exception) {
            outputText.text = "执行失败: ${e.message}"
        }
    }

    private fun updateStatus(text: String) {
        statusText.text = "状态: $text"
    }

    override fun onDestroy() {
        super.onDestroy()
        Shizuku.removeRequestPermissionResultListener(permissionListener)
    }
}
