package com.demo.bridge

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class EditCommandActivity : AppCompatActivity() {
    private lateinit var etContent: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_command)

        etContent = findViewById(R.id.etContent)
        val btnSave = findViewById<Button>(R.id.btnSave)

        val path = intent.getStringExtra("path")
        if (path != null) {
            val file = File(path)
            if (file.exists()) {
                etContent.setText(file.readText())
            }
        }

        btnSave.setOnClickListener {
            if (path == null) {
                Toast(this, "路径为空", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            File(path).writeText(etContent.text.toString())
            Toast(this, "已保存", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
