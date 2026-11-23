package com.demo.bridge

import android.app.Activity
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast

class LogListActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val tv = TextView(this)
        setContentView(tv)
        val logs = LogStore.list(this)
        if (logs.isEmpty()) { tv.text = "暂无日志"; return }
        tv.text = logs.joinToString("\n\n") { "${it.time} ${it.cmdName} ${it.mode} ret=${it.retCode}" }
        tv.setOnClickListener {
            if (logs.isNotEmpty()) {
                val l = logs.first()
                AlertDialog.Builder(this).setTitle(l.cmdName).setMessage(
                    "时间：${l.time}\n模式：${l.mode}\n返回码：${l.retCode}\n输出：${l.stdout}\n错误：${l.stderr}"
                ).setPositiveButton("确定", null).show()
            }
        }
    }
}
