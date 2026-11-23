package com.demo.bridge

import android.app.AlertDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity

class LogListActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var adapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log_list)

        listView = findViewById(R.id.logListView)
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf())
        listView.adapter = adapter

        loadLogs()

        listView.setOnItemClickListener { _, _, position, _ ->
            val text = adapter.getItem(position) ?: return@setOnItemClickListener
            AlertDialog.Builder(this)
                .setTitle("执行日志")
                .setMessage(text)
                .setPositiveButton("关闭", null)
                .show()
        }
    }

    private fun loadLogs() {
        adapter.clear()
        LogStore.all().forEach { log ->
            adapter.add(
                """
                时间: ${log.time}
                命令: ${log.cmdName}
                模式: ${log.mode}
                返回码: ${log.retCode}
                输出: ${log.stdout}
                错误: ${log.stderr}
                """.trimIndent()
            )
        }
        adapter.notifyDataSetChanged()
    }
}
