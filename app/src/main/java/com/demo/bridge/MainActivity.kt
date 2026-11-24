package com.demo.bridge

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var tvMode: TextView
    private lateinit var listContainer: LinearLayout
    private val items = mutableListOf<CommandItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 32, 24, 16)
        }

        // 顶部：标题 + 模式 + 设置
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, 16)
        }

        val title = TextView(this).apply {
            text = "Bridge"
            textSize = 22f
            setTextColor(0xFF111111.toInt())
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        tvMode = TextView(this).apply {
            text = BridgeExecutor.modeLabel()
            textSize = 12f
            setTextColor(0xFF2E7D32.toInt())
            setPadding(12, 0, 12, 0)
        }

        val btnSettings = Button(this).apply {
            text = "设置"
            setOnClickListener {
                try {
                    startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
                } catch (_: Throwable) {
                    Toast.makeText(this@MainActivity, "设置页面未实现", Toast.LENGTH_SHORT).show()
                }
            }
        }

        header.addView(title)
        header.addView(tvMode)
        header.addView(btnSettings)
        root.addView(header)

        // 操作栏：添加 + 导入导出
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, 12)
        }

        val btnAdd = Button(this).apply {
            text = "+ 添加"
            setOnClickListener {
                try {
                    startActivity(Intent(this@MainActivity, EditCommandActivity::class.java))
                } catch (_: Throwable) {
                    Toast.makeText(this@MainActivity, "编辑页面未实现", Toast.LENGTH_SHORT).show()
                }
            }
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

        // 命令列表
        listContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        val scroll = ScrollView(this).apply {
            addView(listContainer)
        }
        root.addView(scroll)

        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        BridgeExecutor.invalidate()
        BridgeExecutor.refreshModeLabel(this) { label ->
            tvMode.text = label
        }
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
            val empty = TextView(this).apply {
                text = "暂无命令，点“+ 添加”创建"
                textSize = 14f
                setTextColor(0xFF888888.toInt())
                setPadding(0, 24, 0, 0)
            }
            listContainer.addView(empty)
            return
        }

        items.forEach { item ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, 8, 0, 8)
            }

            val tv = TextView(this).apply {
                text = item.name
                textSize = 16f
                setTextColor(0xFF222222.toInt())
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val btnRun = Button(this).apply {
                text = "执行"
                setOnClickListener { runItem(item) }
            }

            val btnDel = Button(this).apply {
                text = "删除"
                setOnClickListener {
                    BridgeData.removeItem(this@MainActivity, item.name)
                    loadData()
                    renderList()
                }
            }

            row.addView(tv)
            row.addView(btnRun)
            row.addView(btnDel)
            listContainer.addView(row)
        }
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
