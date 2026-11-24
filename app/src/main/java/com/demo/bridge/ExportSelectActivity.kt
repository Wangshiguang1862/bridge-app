package com.demo.bridge

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ExportSelectActivity : AppCompatActivity() {

    private lateinit var editJson: EditText
    private lateinit var btnExport: Button
    private lateinit var btnImport: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_export_select)

        editJson = findViewById(R.id.editJson)
        btnExport = findViewById(R.id.btnExport)
        btnImport = findViewById(R.id.btnImport)

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
