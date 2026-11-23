package com.demo.bridge
import android.app.Activity
import android.os.Bundle
import android.widget.TextView
class LogListActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        setContentView(TextView(this).apply { text = "日志（待实现）"; textSize = 18f; setPadding(16,16,16,16) })
    }
}
