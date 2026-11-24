package com.demo.bridge

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast

class ExportSelectActivity : Activity() {

    private lateinit var editJson: EditText
    private lateinit var btnExport: Button
    private lateinit var btnImport: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.TOP
            setPadding(24, 24, 24, 24)
        }

        editJson = EditText(this).apply {
            hint = "命令 JSON"
            minLines = 6
            maxLines = 12
        }
        root.addView(editJson)

        btnExport = Button(this).apply { text = "导出当前命令" }
        root.addView(btnExport)

        btnImport = Button(this).apply { text = "从上方 JSON 导入" }
        root.addView(btnImport)

        setContentView(root)

        btnExport.setOnClickListener {
            val json = BridgeData.toJson(BridgeData.loadItems(this))
            editJson.setText(json)
        }

        btnImport.setOnClickListener {
            val parsed = BridgeData.parseItems(editJson.text.toString())
            if (parsed == null) {
                Toast.makeText(this, "JSON 解析失败", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            BridgeData.saveItems(this, parsed)
            Toast.makeText(this, "导入成功，共 ${parsed.size} 条", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
