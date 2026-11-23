package com.demo.bridge

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class MainActivity : Activity() {

    private val REQ_EDIT = 1001
    private val REQ_SHIZUKU_PERM = 1001
    private val items = mutableListOf<CommandItem>()
    private val adapter = CommandAdapter()
    private lateinit var listView: ListView
    private lateinit var tvStatus: TextView
    private lateinit var tvStatusDot: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFFF5F5F5.toInt())
        }

        // ===== 顶部栏 =====
        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(16, 14, 16, 14)
            setBackgroundColor(0xFFFFFFFF.toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        val tvTitle = TextView(this).apply {
            text = "Bridge"
            textSize = 20f
            setTextColor(0xFF222222.toInt())
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        topBar.addView(tvTitle)

        val statusLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setPadding(16, 0, 0, 0)
        }
        tvStatusDot = TextView(this).apply {
            text = "●"
            textSize = 12f
            setTextColor(0xFF999999.toInt())
            setPadding(0, 0, 6, 0)
        }
        tvStatus = TextView(this).apply {
            text = "运行模式：检测中..."
            textSize = 13f
            setTextColor(0xFF666666.toInt())
        }
        statusLayout.addView(tvStatusDot)
        statusLayout.addView(tvStatus)
        topBar.addView(statusLayout)

        val btnSettings = Button(this).apply {
            text = "设置"
            textSize = 13f
            setPadding(12, 6, 12, 6)
            setOnClickListener { startActivity(Intent(this@MainActivity, SettingsActivity::class.java)) }
        }
        topBar.addView(btnSettings)
        root.addView(topBar)
        root.addView(divider())

        // ===== 添加栏 =====
        val addBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(16, 10, 16, 10)
            setBackgroundColor(0xFFFFFFFF.toInt())
        }
        val btnAdd = Button(this).apply {
            text = "+ 添加"
            textSize = 14f
            setPadding(10, 4, 10, 4)
            setOnClickListener { showAddPage() }
        }
        addBar.addView(btnAdd)
        val tvListTitle = TextView(this).apply {
            text = "命令列表"
            textSize = 15f
            setTextColor(0xFF333333.toInt())
            setPadding(12, 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        addBar.addView(tvListTitle)
        root.addView(addBar)
        root.addView(divider())

        // ===== 列表 =====
        listView = ListView(this).apply {
            dividerHeight = 1
            setDivider(resources.getDrawable(android.R.drawable.divider_horizontal_bright))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
            )
        }
        root.addView(listView)

        setContentView(root)
        listView.adapter = adapter
        loadData()
        if (items.isEmpty()) {
            items.addAll(defaultItems())
            saveData()
            adapter.notifyDataSetChanged()
        }

        detectMode()
    }

    private fun divider() = View(this).apply {
        setBackgroundColor(0xFFDDDDDD.toInt())
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)
    }

    private fun detectMode() {
        try {
            if (!rikka.shizuku.Shizuku.pingBinder()) {
                setStatus(0xFF999999.toInt(), "运行模式：基础（Shizuku 未运行）")
                return
            }
            if (rikka.shizuku.Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                setStatus(0xFFFF9800.toInt(), "运行模式：等待 Shizuku 授权...")
                if (rikka.shizuku.Shizuku.shouldShowRequestPermissionRationale()) {
                    AlertDialog.Builder(this)
                        .setTitle("需要 Shizuku 授权")
                        .setMessage("Bridge 需要 Shizuku 权限来执行命令，请在弹出的授权框中点击允许。")
                        .setPositiveButton("去授权") { _, _ ->
                            rikka.shizuku.Shizuku.requestPermission(REQ_SHIZUKU_PERM)
                        }
                        .setNegativeButton("使用基础模式") { _, _ ->
                            setStatus(0xFF999999.toInt(), "运行模式：基础")
                        }
                        .show()
                } else {
                    rikka.shizuku.Shizuku.requestPermission(REQ_SHIZUKU_PERM)
                }
                return
            }
            setStatus(0xFF3DDC84.toInt(), "运行模式：shizuku")
        } catch (e: Exception) {
            setStatus(0xFF999999.toInt(), "运行模式：基础")
        }
    }

    private fun setStatus(color: Int, text: String) {
        tvStatusDot.setTextColor(color)
        tvStatus.text = text
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_SHIZUKU_PERM) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setStatus(0xFF3DDC84.toInt(), "运行模式：shizuku")
                Toast.makeText(this, "Shizuku 授权成功", Toast.LENGTH_SHORT).show()
            } else {
                setStatus(0xFFFF9800.toInt(), "运行模式：adb（Shizuku 未授权）")
                Toast.makeText(this, "Shizuku 授权被拒绝，将使用 adb 模式", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun defaultItems() = listOf(
        CommandItem("打开设置", "com.android.settings", "am start -n com.android.settings/.Settings", false),
        CommandItem("重启系统", "", "reboot", false),
        CommandItem("清后台", "", "am kill-all", false)
    )

    private fun loadData() {
        try {
            val f = File(applicationContext.filesDir, "commands.json")
            if (!f.exists()) return
            items.clear()
            val arr = JSONArray(f.readText())
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                items.add(
                    CommandItem(
                        o.optString("name"),
                        o.optString("pkg"),
                        o.optString("action"),
                        o.optBoolean("enabled", false)
                    )
                )
            }
            adapter.notifyDataSetChanged()
        } catch (e: Exception) {
            Toast.makeText(this, "加载失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun saveData() {
        val arr = JSONArray()
        for (it in items) {
            arr.put(JSONObject().apply {
                put("name", it.name)
                put("pkg", it.pkg)
                put("action", it.action)
                put("enabled", it.enabled)
            })
        }
        File(applicationContext.filesDir, "commands.json").writeText(arr.toString())
    }

    private fun showAddPage() {
        startActivityForResult(Intent(this, EditCommandActivity::class.java), REQ_EDIT)
    }

    private fun showItemMenu(item: CommandItem, pos: Int) {
        AlertDialog.Builder(this)
            .setTitle(item.name)
            .setItems(arrayOf("编辑", "导入/导出", "运行", "删除")) { _, w ->
                when (w) {
                    0 -> startActivityForResult(Intent(this, EditCommandActivity::class.java).apply {
                        putExtra("name", item.name)
                        putExtra("pkg", item.pkg)
                        putExtra("action", item.action)
                    }, REQ_EDIT)
                    1 -> Toast.makeText(this, "导入/导出：后续版本", Toast.LENGTH_SHORT).show()
                    2 -> {
                        val log = CommandRunner.run(this, item, true)
                        AlertDialog.Builder(this)
                            .setTitle("执行结果")
                            .setMessage("返回码: ${log.retCode}\n输出: ${log.stdout}\n错误: ${log.stderr}")
                            .setPositiveButton("确定", null).show()
                    }
                    3 -> {
                        items.removeAt(pos)
                        saveData()
                        adapter.notifyDataSetChanged()
                    }
                }
            }.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_EDIT && resultCode == RESULT_OK && data != null) {
            val name = data.getStringExtra("name") ?: return
            val pkg = data.getStringExtra("pkg") ?: ""
            val action = data.getStringExtra("action") ?: ""
            val isEdit = data.getBooleanExtra("isEdit", false)
            val originalName = data.getStringExtra("originalName") ?: ""
            if (isEdit) {
                val idx = items.indexOfFirst { it.name == originalName }
                if (idx >= 0) items[idx] = CommandItem(name, pkg, action, items[idx].enabled)
            } else {
                items.add(CommandItem(name, pkg, action, false))
            }
            saveData()
            adapter.notifyDataSetChanged()
        }
    }

    data class CommandItem(val name: String, val pkg: String, val action: String, var enabled: Boolean)

    private inner class CommandAdapter : BaseAdapter() {

        private class VH {
            lateinit var cb: CheckBox
            lateinit var seq: TextView
            lateinit var name: TextView
            lateinit var more: Button
            var pos: Int = 0
            var listener: CompoundButton.OnCheckedChangeListener? = null
        }

        override fun getCount() = items.size
        override fun getItem(p: Int) = items[p]
        override fun getItemId(p: Int) = p.toLong()

        override fun getView(p: Int, cv: View?, parent: ViewGroup?): View {
            val item = items[p]
            val row: View
            val h: VH
            if (cv == null) {
                row = LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(16, 12, 16, 12)
                }
                h = VH()
                h.cb = CheckBox(this@MainActivity).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { rightMargin = 12 }
                }
                h.seq = TextView(this@MainActivity).apply {
                    textSize = 15f
                    setTextColor(0xFF888888.toInt())
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { rightMargin = 8 }
                }
                h.name = TextView(this@MainActivity).apply {
                    textSize = 16f
                    setTextColor(0xFF222222.toInt())
                    layoutParams = LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                    )
                }
                h.more = Button(this@MainActivity).apply {
                    text = "…"
                    textSize = 18f
                    setPadding(8, 0, 8, 0)
                    minimumWidth = 0
                    minWidth = 0
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }
                row.addView(h.cb)
                row.addView(h.seq)
                row.addView(h.name)
                row.addView(h.more)
                row.tag = h
            } else {
                row = cv
                h = row.tag as VH
            }

            h.pos = p
            h.listener?.let { l -> h.cb.setOnCheckedChangeListener(null) }
            h.cb.isChecked = item.enabled
            h.listener = CompoundButton.OnCheckedChangeListener { _, checked ->
                val idx = h.pos
                if (idx in items.indices) {
                    items[idx].enabled = checked
                    saveData()
                }
            }
            h.cb.setOnCheckedChangeListener(h.listener)

            h.seq.text = "${p + 1}."
            h.name.text = item.name
            h.more.setOnClickListener { showItemMenu(item, p) }
            row.setBackgroundColor(if (p % 2 == 0) 0xFFFFFFFF.toInt() else 0xFFFAFAFA.toInt())
            return row
        }
    }
}
