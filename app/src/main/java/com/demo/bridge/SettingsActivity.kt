package com.demo.bridge
import android.app.Activity
import android.os.Bundle
import android.widget.*
class SettingsActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val tv = TextView(this).apply { text = "设置（待实现）"; textSize = 18f; setPadding(16,16,16,16) }
        setContentView(tv)
    }
}
