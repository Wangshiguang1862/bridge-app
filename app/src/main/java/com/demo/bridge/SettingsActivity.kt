package com.demo.bridge

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    private lateinit var etShizukuTimeout: EditText
    private lateinit var etExternalToken: EditText
    private lateinit var btnSave: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        etShizukuTimeout = findViewById(R.id.etShizukuTimeout)
        etExternalToken = findViewById(R.id.etExternalToken)
        btnSave = findViewById(R.id.btnSave)

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
