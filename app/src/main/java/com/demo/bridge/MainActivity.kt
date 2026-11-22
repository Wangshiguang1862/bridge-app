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

    private var statusText: TextView? = null
    private var authButton: Button? = null
    private var execButton: Button? = null
    private var commandInput: EditText? = null
    private var outputText: TextView? = null
    private var outputScroll: ScrollView? = null

    private val requestCode = 1001

    private val permissionListener = Shizuku.OnRequestPermissionResultListener { req, grant ->
        if (req == requestCode) {
            runOnUiThread {
                if (grant == PackageManager.PERMISSION_GRANTED) {
                    statusText?.text = "状态: Shizuku 已授权"
                    execButton?.isEnabled = true
                    Toast.makeText(this, "Shizuku 授权成功", Toast.LENGTH_SHORT).show()
                } else {
                    statusText?.text = "状态: Shizuku 授权被拒绝"
                    execButton?.isEnabled = false
                    Toast.makeText(this, "Shizuku 授权被拒绝", Toast.LENGTH_SHORT).show()
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

        statusText = TextView(this).apply { text = "状态: 检测中..." }
        authButton = Button(this).apply { text = "请求 Shizuku 授权" }
        commandInput = EditText(this).apply { hint = "输入命令，如 pm list packages" }
        execButton = Button(this).apply { text = "执行" }
        outputText = TextView(this).apply { isTextSelectable = true }
        outputScroll = ScrollView(this).apply { addView(outputText) }

        root.addView(statusText)
        root.addView(authButton)
        root.addView(commandInput)
        root.addView(execButton)
        root.addView(outputScroll)

        setContentView(root)

        Shizuku.addRequestPermissionResultListener(permissionListener)

        authButton?.setOnClickListener {
            try {
                val ready = Shizuku.pingBinder()
                if (!ready) {
                    statusText?.text = "状态: Shizuku 未安装或未启动"
                    Toast.makeText(this, "Shizuku 未就绪", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                    statusText?.text = "状态: Shizuku 已授权"
                    execButton?.isEnabled = true
                    Toast.makeText(this, "Shizuku 已授权", Toast.LENGTH_SHORT).show()
                } else {
                    statusText?.text = "状态: 请求授权中..."
                    Shizuku.requestPermission(requestCode)
                }
            } catch (e: Throwable) {
                statusText?.text = "状态: Shizuku 检测失败: ${e.message}"
            }
        }

        execButton?.setOnClickListener {
            val cmd = commandInput?.text?.toString()?.trim().orEmpty()
            outputText?.text = if (cmd.isEmpty()) "命令不能为空" else "待执行: $cmd"
        }

        checkShizukuStatus()
    }

    override fun onDestroy() {
        try {
            Shizuku.removeRequestPermissionResultListener(permissionListener)
        } catch (e: Throwable) {
            // 忽略
        }
        super.onDestroy()
    }

    private fun checkShizukuStatus() {
        try {
            if (!Shizuku.pingBinder()) {
                statusText?.text = "状态: Shizuku 未安装或未启动"
                authButton?.isEnabled = true
                execButton?.isEnabled = false
                return
            }
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                statusText?.text = "状态: Shizuku 已授权"
                authButton?.isEnabled = false
                execButton?.isEnabled = true
            } else {
                statusText?.text = "状态: Shizuku 已安装，未授权"
                authButton?.isEnabled = true
                execButton?.isEnabled = false
            }
        } catch (e: Throwable) {
            statusText?.text = "状态: Shizuku 检测失败: ${e.message}"
        }
    }
}
