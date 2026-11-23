package com.demo.bridge

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class EditCommandActivity : Activity() {

    private lateinit var etName: EditText
    private lateinit var etPkg: EditText
    private lateinit var etAction: EditText
    private lateinit var btnSave: Button

    private var isEdit = false
    private var originalName = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        root.addView(TextView(this).apply {
            text = "编辑命令"
            textSize = 20f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 24)
        })

        etName = EditText(this).apply { hint = "命令名称（必填）" }
        etPkg = EditText(this).apply { hint = "包名（可选，如 com.android.settings）" }
        etAction = EditText(this).apply {
            hint = "执行命令或 Activity（必填，如 am start -n ...）"
            minLines = 3
        }
        btnSave = Button(this).apply { text = "保存" }

        root.addView(etName)
        root.addView(etPkg)
        root.addView(etAction)
        root.addView(btnSave)

        setContentView(root)

        // 如果是编辑模式，填充已有数据
        intent?.let {
            if (it.hasExtra("name")) {
                isEdit = true
                originalName = it.getStringExtra("name") ?: ""
                etName.setText(originalName)
                etPkg.setText(it.getStringExtra("pkg") ?: "")
                etAction.setText(it.getStringExtra("action") ?: "")
            }
        }

        btnSave.setOnClickListener {
            val name = etName.text.toString().trim()
            val pkg = etPkg.text.toString().trim()
            val action = etAction.text.toString().trim()

            if (name.isEmpty() || action.isEmpty()) {
                Toast.makeText(this, "命令名称和执行内容不能为空", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 把结果返回给 MainActivity
            val result = Intent().apply {
                putExtra("name", name)
                putExtra("pkg", pkg)
                putExtra("action", action)
                putExtra("isEdit", isEdit)
                putExtra("originalName", originalName)
            }
            setResult(RESULT_OK, result)
            finish()
        }
    }
}
