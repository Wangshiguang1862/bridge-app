package com.demo.bridge

import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
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
    private lateinit var commandInput: EditText
    private lateinit var execButton: Button
    private lateinit var outputText: TextView
    private lateinit var outputScroll: ScrollView

    private val REQUEST_CODE = 1001

    private val permissionListener = object : Shizuku.OnRequestPermissionResultListener {
        override fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {
            runOnUiThread {
                if (grantResult == PackageManager.PERMISSION_GRANTED) {
                    statusText.text = "状态: Shizuku 已授权"
                    authButton.isEnabled = false
                    execButton.isEnabled = true
                } else {
                    statusText.text = "状态: Shizuku 授权被拒绝"
                    Toast.makeText(this@MainActivity, "授权被拒绝", Toast.LENGTH_SHORT).show()
                }
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
        if (!isShizukuReady() || Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            outputText.text = "Shizuku 未就绪或未授权"
            return
        }
        try {
            val remoteProcess = Shizuku.newProcess(
                arrayOf("sh", "-c", command),
                null,
                null
            )
            val result = remoteProcess.inputStream.bufferedReader().readText()
            val error = remoteProcess.errorStream.bufferedReader().readText()
            remoteProcess.waitFor()

            val output = buildString {
                if (result.isNotBlank()) append(result)
                if (error.isNotBlank()) append("\n错误:\n$error")
            }
            outputText.text = output.ifBlank { "执行完成，无输出" }
            outputScroll.post { outputScroll.fullScroll(ScrollView.FOCUS_DOWN) }
        } catch (e: Exception) {
            outputText.text = "Shizuku 执行失败: ${e.message}"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            Shizuku.removeRequestPermissionResultListener(permissionListener)
        } catch (e: Exception) {
            // ignore
        }
    }
}
