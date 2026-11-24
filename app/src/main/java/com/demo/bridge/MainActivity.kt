package com.demo.bridge

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var tvMode: TextView
    private lateinit var tvList: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 20, 16, 12)
        }
        setContentView(root)

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(16, 20, 16, 12)
        }

        val title = TextView(this).apply {
            text = "Bridge"
            textSize = 22f
            setTextColor(0xFF111111.toInt())
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        tvMode = TextView(this).apply {
            text = BridgeExecutor.modeLabel()
            textSize = 13f
            setTextColor(0xFF2E7D32.toInt())
        }

        val btnSet = Button(this).apply {
            text = "设置"
            setOnClickListener {
                startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
            }
        }

        header.addView(title)
        header.addView(tvMode)
        header.addView(btnSet)
        root.addView(header)

        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(16, 8, 16, 8)
            setBackgroundColor(0xFFFFFFFF.toInt())
        }

        val btnAdd = Button(this).apply {
            text = "+ 添加"
            setOnClickListener {
                // 你原有添加命令跳转逻辑保留
            }
        }

        tvList = TextView(this).apply {
            text = "命令列表"
            textSize = 17f
            setTextColor(0xFF222222.toInt())
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        bar.addView(btnAdd)
        bar.addView(tvList)
        root.addView(bar)

        // ==== 你原有命令列表渲染、点击执行逻辑放这里 ====
        // 执行调用保持：BridgeExecutor.exec(item.action)
    }

    override fun onResume() {
        super.onResume()
        BridgeExecutor.invalidate()
        BridgeExecutor.refreshModeLabel(this) { label ->
            tvMode.text = label
        }
        // 你原有刷新列表逻辑放这里
    }
}
