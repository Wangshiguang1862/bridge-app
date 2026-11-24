package com.demo.bridge

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.widget.*

class SettingsActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "设置"
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
        }
        val btnExport = Button(this).apply {
    text = "导出命令"
    setOnClickListener {
        startActivity(Intent(this@SettingsActivity, ExportSelectActivity::class.java))
    }
}
        root.addView(TextView(this).apply { text = "运行模式：Shizuku（只读配置阶段）" })
        root.addView(TextView(this).apply {
            text = "本版本仅保证编译与列表管理，真实执行在下一步接入。"
            setPadding(0, 16, 0, 0)
        })
        root.addView(Button(this).apply {
            text = "返回"
            setOnClickListener { finish() }
            setPadding(0, 24, 0, 0)
        })
        setContentView(root)
    }
}
