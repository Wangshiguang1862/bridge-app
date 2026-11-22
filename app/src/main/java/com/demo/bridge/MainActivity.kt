package com.demo.bridge

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var authButton: Button
    private lateinit var execButton: Button
    private lateinit var commandInput: EditText
    private lateinit var outputText: TextView
    private lateinit var outputScroll: ScrollView

    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 纯代码布局，不依赖 xml
        val root = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
        }

        statusText = TextView(this).apply { text = "状态: 界面就绪" }
        authButton = Button(this).apply { text = "请求 Shizuku 授权" }
        commandInput = EditText(this).apply { hint = "输入命令，如 pm list packages" }
        execButton = Button(this).apply { text = "执行" }
        outputText = TextView(this).apply { setTextIsSelectable(true) }
        outputScroll = ScrollView(this).apply {
            addView(outputText)
        }

        root.addView(statusText)
        root.addView(authButton)
        root.addView(commandInput)
        root.addView(execButton)
        root.addView(outputScroll)

        setContentView(root)

        authButton.setOnClickListener {
            statusText.text = "状态: 授权功能待接入"
            execButton.isEnabled = true
            Toast.makeText(this, "授权功能下一步接入", Toast.LENGTH_SHORT).show()
        }

        execButton.setOnClickListener {
            val cmd = commandInput.text.toString().trim()
            outputText.text = if (cmd.isEmpty()) "命令不能为空" else "待执行: $cmd"
        }
    }
}
