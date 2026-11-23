package com.demo.bridge

import android.app.Activity
import android.os.Bundle
import android.widget.*

class EditCommandActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = if (intent.getBooleanExtra("isEdit", false)) "编辑命令" else "添加命令"

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
        }
        val etName = EditText(this).apply { hint = "名称" }
        val etPkg = EditText(this).apply { hint = "包名" }
        val etAction = EditText(this).apply { hint = "动作/命令" }
        etName.setText(intent.getStringExtra("name") ?: "")
        etPkg.setText(intent.getStringExtra("pkg") ?: "")
        etAction.setText(intent.getStringExtra("action") ?: "")

        root.addView(etName); root.addView(etPkg); root.addView(etAction)
        root.addView(Button(this).apply {
            text = "保存"
            setOnClickListener {
                intent.putExtra("name", etName.text.toString())
                intent.putExtra("pkg", etPkg.text.toString())
                intent.putExtra("action", etAction.text.toString())
                setResult(RESULT_OK, intent)
                finish()
            }
        })
        setContentView(root)
    }
}
