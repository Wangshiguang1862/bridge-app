package com.demo.bridge

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var statusText: TextView
    private lateinit var authButton: Button
    private lateinit var execButton: Button
    private lateinit var commandInput: EditText
    private lateinit var outputText: TextView
    private lateinit var outputScroll: ScrollView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
        }

        statusText = TextView(this).apply { text = "状态: 界面就绪" }
        authButton = Button(this).apply { text = "请求 Shizuku 授权" }
        commandInput = EditText(this).apply { hint = "输入命令，如 pm list packages" }
        execButton = Button(this).apply { text = "执行" }
        outputText = TextView(this).apply { isTextSelectable = true }
        outputScroll = ScrollView(this).apply { addView(outputText) }

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
