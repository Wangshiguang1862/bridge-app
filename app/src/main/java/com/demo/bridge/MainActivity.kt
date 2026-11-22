package com.demo.bridge

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import rikka.shizuku.Shizuku

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var requestBtn: Button
    private lateinit var runBtn: Button

    private val requestCode = 1001

    private val permissionListener = Shizuku.OnRequestPermissionResultListener { _, grantResult ->
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
            if (Shizuku.isPreV11() || Shizuku.getVersion() < 11) {
                updateStatus()
            } else if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                Shizuku.requestPermission(requestCode)
            } else {
                updateStatus()
            }
        }

        runBtn.setOnClickListener {
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                try {
                    val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", "id"))
                    val result = process.inputStream.bufferedReader().use { it.readText() }
                    statusText.text = result
                } catch (e: Exception) {
                    statusText.text = e.message
                }
            } else {
                statusText.text = "未授权"
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
