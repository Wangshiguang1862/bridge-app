package com.demo.bridge

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import rikka.shizuku.Shizuku

class MainActivity : Activity() {

    private lateinit var statusText: TextView
    private lateinit var requestBtn: Button
    private lateinit var runBtn: Button

    private val requestCode = 1001

    private val permissionListener =
        Shizuku.OnRequestPermissionResultListener { _, grantResult ->
            updateStatus()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        requestBtn = findViewById(R.id.requestBtn)
        runBtn = findViewById(R.id.runBtn)

        Shizuku.addRequestPermissionResultListener(permissionListener)

        requestBtn.setOnClickListener {
            when {
                Shizuku.isPreV11() || Shizuku.getVersion() < 11 -> {
                    statusText.text = "Shizuku 版本过低"
                }
                Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED -> {
                    Shizuku.requestPermission(requestCode)
                }
                else -> updateStatus()
            }
        }

        runBtn.setOnClickListener {
            if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                statusText.text = "未授权"
                return@setOnClickListener
            }

            try {
                val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", "id"))
                val result = process.inputStream.bufferedReader().use { it.readText() }
                statusText.text = result.ifBlank { "命令执行成功，无输出" }
            } catch (e: Exception) {
                statusText.text = e.message ?: "执行失败"
            }
        }

        updateStatus()
    }

    private fun updateStatus() {
        val granted = Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        statusText.text = if (granted) "Shizuku 已授权" else "Shizuku 未授权"
        runBtn.isEnabled = granted
    }

    override fun onDestroy() {
        super.onDestroy()
        Shizuku.removeRequestPermissionResultListener(permissionListener)
    }
}
