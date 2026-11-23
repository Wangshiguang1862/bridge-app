package com.demo.bridge
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.*
class EditCommandActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val etName = EditText(this).apply { hint = "名称" }
        val etPkg = EditText(this).apply { hint = "包名" }
        val etAct = EditText(this).apply { hint = "动作" }
        val btn = Button(this).apply {
            text = "保存"
            setOnClickListener {
                setResult(RESULT_OK, Intent().apply {
                    putExtra("name", etName.text.toString())
                    putExtra("pkg", etPkg.text.toString())
                    putExtra("action", etAct.text.toString())
                    putExtra("isEdit", intent.getBooleanExtra("isEdit", false))
                    putExtra("originalName", intent.getStringExtra("originalName") ?: "")
                })
                finish()
            }
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16,16,16,16)
            addView(etName); addView(etPkg); addView(etAct); addView(btn)
        }
        intent.getStringExtra("name")?.let { etName.setText(it) }
        intent.getStringExtra("pkg")?.let { etPkg.setText(it) }
        intent.getStringExtra("action")?.let { etAct.setText(it) }
        setContentView(root)
    }
}
