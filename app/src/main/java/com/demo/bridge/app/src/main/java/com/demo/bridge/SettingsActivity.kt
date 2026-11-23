package com.demo.bridge

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView

class SettingsActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val tv = TextView(this).apply { text = "设置\n\n当前模式：${GlobalState.mode}" }
        val btnRefresh = Button(this).apply { text = "重新检测" }
        val btnClear = Button(this).apply { text = "清空命令" }
        val btnExport = Button(this).apply { text = "导出全部" }
        val btnLogs = Button(this).apply { text = "执行日志" }
        val btnAbout = Button(this).apply { text = "关于" }
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            addView(tv); addView(btnRefresh); addView(btnClear); addView(btnExport); addView(btnLogs); addView(btnAbout)
        }
        setContentView(layout)

        btnRefresh.setOnClickListener {
            (this@SettingsActivity as? Activity)?.let { (this@SettingsActivity as Activity).recreate() }
            tv.text = "当前模式：${GlobalState.mode}"
        }
        btnClear.setOnClickListener {
            AlertDialog.Builder(this).setTitle("确认清空").setMessage("将删除全部命令，不可恢复")
                .setPositiveButton("确定") { _, _ ->
                    deleteFile("commands.json")
                    Toast.makeText(this, "已清空", Toast.LENGTH_SHORT).show()
                }.setNegativeButton("取消", null).show()
        }
        btnExport.setOnClickListener { Toast.makeText(this, "导出：后续版本", Toast.LENGTH_SHORT).show() }
        btnLogs.setOnClickListener { startActivity(Intent(this, LogListActivity::class.java)) }
        btnAbout.setOnClickListener {
            AlertDialog.Builder(this).setTitle("关于").setMessage("Bridge v0.1\n接口：com.demo.bridge.EXECUTE").setPositiveButton("确定", null).show()
        }
    }
}
