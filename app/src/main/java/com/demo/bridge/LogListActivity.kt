package com.demo.bridge

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView

class LogListActivity : Activity() {

    private lateinit var listView: ListView
    private lateinit var adapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 兜底布局，避免依赖不存在的 activity_log_list.xml
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(TextView(this@LogListActivity).apply { text = "执行日志" })
        }
        listView = ListView(this).apply { id = android.R.id.list }
        root.addView(listView, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0, 1f
        ))
        setContentView(root)

        adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            ArrayList<String>()
        )
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
