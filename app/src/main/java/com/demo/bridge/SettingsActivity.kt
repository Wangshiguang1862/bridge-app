package com.demo.bridge

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast

class SettingsActivity : Activity() {

    private lateinit var etShizukuTimeout: EditText
    private lateinit var etExternalToken: EditText
    private lateinit var btnSave: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        etShizukuTimeout = EditText(this).apply { hint = "Shizuku超时(秒)" }
        etExternalToken = EditText(this).apply { hint = "外部Token" }
        btnSave = Button(this).apply { text = "保存" }
        root.addView(etShizukuTimeout)
        root.addView(etExternalToken)
        root.addView(btnSave)
        setContentView(root)

        val prefs = getSharedPreferences("bridge_settings", MODE_PRIVATE)
        etShizukuTimeout.setText(prefs.getInt("shizuku_timeout", 10).toString())
        etExternalToken.setText(prefs.getString("external_token", "").orEmpty())

        btnSave.setOnClickListener {
            val timeout = etShizukuTimeout.text.toString().toIntOrNull() ?: 10
            val token = etExternalToken.text.toString()
            prefs.edit()
                .putInt("shizuku_timeout", timeout)
                .putString("external_token", token)
                .apply()
            Toast.makeText(this, "设置已保存", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
