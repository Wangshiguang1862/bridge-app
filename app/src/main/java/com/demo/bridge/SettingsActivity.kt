package com.demo.bridge

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class SettingsActivity : Activity() {
    companion object {
        const val REQ_IMPORT_FILE = 3001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "设置"

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
        }

        val btnExport = Button(this).apply {
            text = "选择性导出命令"
            setOnClickListener {
                startActivity(Intent(this@SettingsActivity, ExportSelectActivity::class.java))
            }
        }

        val btnImportFile = Button(this).apply {
            text = "从文件导入（清空并替换）"
            setOnClickListener {
                val i = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "application/json"
                }
                startActivityForResult(i, REQ_IMPORT_FILE)
            }
        }

        val tip1 = TextView(this).apply {
            text = "运行模式：Shizuku（只读接口）"
            setPadding(0, 24, 0, 8)
        }
        val tip2 = TextView(this).apply {
            text = "本版本仅保证编译与列表管理，真实执行在下一步接入。"
            setPadding(0, 0, 0, 8)
        }
        val tip3 = TextView(this).apply {
            text = "建议：主列表长按可粘贴导入或清空全部。"
            setPadding(0, 0, 0, 8)
        }

        root.addView(btnExport)
        root.addView(btnImportFile)
        root.addView(tip1)
        root.addView(tip2)
        root.addView(tip3)
        setContentView(root)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_IMPORT_FILE && resultCode == RESULT_OK && data != null) {
            try {
                val uri = data.data ?: return
                val txt = contentResolver.openInputStream(uri)?.use {
                    it.bufferedReader().readText()
                } ?: return
                if (txt.isBlank()) {
                    Toast.makeText(this, "文件为空", Toast.LENGTH_SHORT).show()
                    return
                }
                val arr = org.json.JSONArray(txt)
                val prefs = getSharedPreferences("bridge_prefs", MODE_PRIVATE)
                prefs.edit().putString("commands", arr.toString()).apply()
                Toast.makeText(this, "已导入，重启或刷新列表生效", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(this, "导入失败：${e.javaClass.simpleName}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
