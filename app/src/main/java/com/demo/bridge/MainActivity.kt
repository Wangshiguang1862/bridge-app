package com.demo.bridge

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 示例：filesDir 只能在 Activity/Context 里用
        val dir = filesDir.absolutePath

        // 示例：finish() 只能在 Activity 里用
        // finish()

        // 示例：lambda 显式写参数，避免 it 推断失败
        listOf("a", "b").forEach { item ->
            Toast.makeText(this, item, Toast.LENGTH_SHORT).show()
        }
    }
}
