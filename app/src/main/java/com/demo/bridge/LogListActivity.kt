package com.demo.bridge

import android.app.Activity
import android.os.Bundle
import android.widget.*

class LogListActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "执行日志"
        val lv = ListView(this)
        lv.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, listOf("暂无日志"))
        setContentView(lv)
    }
}
