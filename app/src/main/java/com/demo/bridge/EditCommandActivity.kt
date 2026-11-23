package com.demo.bridge

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast

class EditCommandActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val etName = EditText(this).apply { hint = "命令名" }
        val etPkg = EditText(this).apply { hint = "包名" }
        val etAction = EditText(this).apply { hint = "执行命令" }
        val btn = Button(this).apply { text = "保存" }
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            addView(etName); addView(etPkg); addView(etAction); addView(btn)
        }
        setContentView(layout)

        intent?.let {
            etName.setText(it.getStringExtra("name"))
            etPkg.setText(it.getStringExtra("pkg"))
            etAction.setText(it.getStringExtra("action"))
        }

        btn.setOnClickListener {
            if (etName.text.isBlank() || etAction.text.isBlank()) {
                Toast.makeText(this, "命令名和执行命令不能为空", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            Toast.makeText(this, "保存功能：后续版本", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
