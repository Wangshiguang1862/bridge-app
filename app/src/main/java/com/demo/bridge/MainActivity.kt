package com.demo.bridge

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private val items = mutableListOf<CommandItem>()
    private lateinit var tvMode: TextView
    private lateinit var listContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
        }

        // 顶部标题栏
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val tvTitle = TextView(this).apply {
            text = "Bridge"
            textSize = 22f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        tvMode = TextView(this).apply { textSize = 13f }
        val btnSettings = Button(this).apply {
            text = "设置"
            setOnClickListener {
                Toast.makeText(this@MainActivity, "设置页暂未实现", Toast.LENGTH_SHORT).show()
            }
        }
        header.addView(tvTitle)
        header.addView(tvMode)
        header.addView(btnSettings)
        root.addView(header)

        // 操作栏
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 12, 0, 12)
        }
        val btnAdd = Button(this).apply {
            text = "+ 添加"
            setOnClickListener { showAddDialog() }
        }
        val btnExport = Button(this).apply {
            text = "导入/导出"
            setOnClickListener {
                startActivity(Intent(this@MainActivity, ExportSelectActivity::class.java))
            }
        }
        bar.addView(btnAdd)
        bar.addView(btnExport)
        root.addView(bar)

        listContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(ScrollView(this).apply { addView(listContainer) })

        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        BridgeExecutor.invalidate()
        BridgeExecutor.refreshModeLabel(this) { label -> tvMode.text = label }
        loadData()
        renderList()
    }

    private fun loadData() {
        items.clear()
        items.addAll(BridgeData.loadItems(this))
    }

    private fun renderList() {
        listContainer.removeAllViews()
        if (items.isEmpty()) {
            listContainer.addView(TextView(this).apply {
                text = "暂无命令，点“+ 添加”创建"
                textSize = 14f
                setPadding(0, 24, 0, 0)
            })
            return
        }
        items.forEach { item ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, 8, 0, 8)
            }
            val tv = TextView(this).apply {
                text = "${item.name}\n${item.action}"
                textSize = 14f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val btnRun = Button(this).apply { text = "执行"; setOnClickListener { runItem(item) } }
            val btnDel = Button(this).apply {
                text = "删除"
                setOnClickListener {
                    BridgeData.removeItem(this@MainActivity, item.name)
                    loadData(); renderList()
                }
            }
            row.addView(tv); row.addView(btnRun); row.addView(btnDel)
            listContainer.addView(row)
        }
    }

    private fun showAddDialog() {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(24, 12, 24, 12) }
        val etName = EditText(this).apply { hint = "命令名称" }
        val etPkg = EditText(this).apply { hint = "包名（可选）" }
        val etAction = EditText(this).apply { hint = "执行命令，如 am start -n ..." }
        box.addView(etName); box.addView(etPkg); box.addView(etAction)

        AlertDialog.Builder(this)
            .setTitle("添加命令")
            .setView(box)
            .setPositiveButton("保存") { _, _ ->
                val name = etName.text.toString().trim()
                val action = etAction.text.toString().trim()
                if (name.isEmpty() || action.isEmpty()) {
                    Toast.makeText(this, "名称和执行命令不能为空", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                BridgeData.addItem(
                    this,
                    CommandItem(name = name, pkg = etPkg.text.toString().trim(), action = action)
                )
                loadData(); renderList()
                Toast.makeText(this, "已添加", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun runItem(item: CommandItem) {
        if (item.action.isBlank()) {
            Toast.makeText(this, "命令为空", Toast.LENGTH_SHORT).show()
            return
        }
        Thread {
            val result = BridgeExecutor.exec(item.action)
            runOnUiThread {
                AlertDialog.Builder(this)
                    .setTitle("执行结果（${BridgeExecutor.modeLabel()}）")
                    .setMessage(result.take(3000))
                    .setPositiveButton("确定", null)
                    .show()
            }
        }.start()
    }
}
