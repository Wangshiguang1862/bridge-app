package com.demo.bridge

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : Activity() {

    companion object {
        const val REQ_EDIT = 1001
        const val PREFS = "bridge_prefs"
        const val KEY_CMDS = "commands"
    }

    private lateinit var listView: ListView
    private lateinit var adapter: CommandAdapter
    private val items = mutableListOf<CommandItem>()

    data class CommandItem(
        val name: String,
        val pkg: String,
        val action: String,
        var enabled: Boolean
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFFF5F5F5.toInt())
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(16, 20, 16, 12)
        }
        val title = TextView(this).apply {
            text = "Bridge"
            textSize = 22f
            setTextColor(0xFF111111.toInt())
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val mode = TextView(this).apply {
            text = "● 运行模式：shizuku"
            textSize = 13f
            setTextColor(0xFF2E7D32.toInt())
        }
        val btnSet = Button(this).apply {
            text = "设置"
            setOnClickListener { startActivity(Intent(this@MainActivity, SettingsActivity::class.java)) }
        }
        header.addView(title); header.addView(mode); header.addView(btnSet)
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
                startActivityForResult(Intent(this@MainActivity, EditCommandActivity::class.java), REQ_EDIT)
            }
        }
        val tvList = TextView(this).apply {
            text = "命令列表"
            textSize = 17f
            setTextColor(0xFF222222.toInt())
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { leftMargin = 12 }
        }
        bar.addView(btnAdd); bar.addView(tvList)
        root.addView(bar)

        listView = ListView(this).apply {
            dividerHeight = 1
            divider = resources.getDrawable(android.R.drawable.divider_horizontal_bright, theme)
        }
        adapter = CommandAdapter()
        listView.adapter = adapter
        root.addView(listView, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ))

        setContentView(root)
        loadData()
        adapter.notifyDataSetChanged()
    }

    private fun prefs() = getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun loadData() {
        items.clear()
        val raw = prefs().getString(KEY_CMDS, "") ?: ""
        if (raw.isBlank()) return
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                items.add(CommandItem(
                    o.optString("name"), o.optString("pkg"),
                    o.optString("action"), o.optBoolean("enabled", false)
                ))
            }
        } catch (_: Exception) {}
    }

    private fun saveData() {
        val arr = JSONArray()
        items.forEach {
            val o = JSONObject()
            o.put("name", it.name); o.put("pkg", it.pkg)
            o.put("action", it.action); o.put("enabled", it.enabled)
            arr.put(o)
        }
        prefs().edit().putString(KEY_CMDS, arr.toString()).apply()
    }

    private fun showItemMenu(item: CommandItem, pos: Int) {
        val opts = arrayOf("编辑", "导入/导出", "测试执行", "删除")
        AlertDialog.Builder(this)
            .setTitle(item.name)
            .setItems(opts) { _, which ->
                when (which) {
                    0 -> startActivityForResult(Intent(this, EditCommandActivity::class.java).apply {
                        putExtra("isEdit", true); putExtra("originalName", item.name)
                        putExtra("name", item.name); putExtra("pkg", item.pkg); putExtra("action", item.action)
                    }, REQ_EDIT)
                    1 -> Toast.makeText(this, "导入/导出：后续版本", Toast.LENGTH_SHORT).show()
                    2 -> BridgeExecutor.testExecute(this, item)
                    3 -> { items.removeAt(pos); saveData(); adapter.notifyDataSetChanged() }
                }
            }.show()
    }

    override fun onActivityResult(req: Int, res: Int, data: Intent?) {
        super.onActivityResult(req, res, data)
        if (req == REQ_EDIT && res == RESULT_OK && data != null) {
            val name = data.getStringExtra("name") ?: return
            val pkg = data.getStringExtra("pkg") ?: ""
            val action = data.getStringExtra("action") ?: ""
            val isEdit = data.getBooleanExtra("isEdit", false)
            val orig = data.getStringExtra("originalName") ?: ""
            if (isEdit) {
                val idx = items.indexOfFirst { it.name == orig }
                if (idx >= 0) items[idx] = CommandItem(name, pkg, action, items[idx].enabled)
            } else items.add(CommandItem(name, pkg, action, false))
            saveData(); adapter.notifyDataSetChanged()
        }
    }

    private class VH {
        lateinit var cb: CheckBox; lateinit var seq: TextView
        lateinit var name: TextView; lateinit var more: Button
        var pos: Int = 0
        var listener: CompoundButton.OnCheckedChangeListener? = null
    }

    private inner class CommandAdapter : BaseAdapter() {
        override fun getCount() = items.size
        override fun getItem(p: Int) = items[p]
        override fun getItemId(p: Int) = p.toLong()

        override fun getView(p: Int, cv: View?, parent: ViewGroup?): View {
            val item = items[p]
            val row: View; val h: VH
            if (cv == null) {
                row = LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = android.view.Gravity.CENTER_VERTICAL
                    setPadding(16, 12, 16, 12)
                }
                h = VH()
                h.cb = CheckBox(this@MainActivity).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { rightMargin = 12 }
                }
                h.seq = TextView(this@MainActivity).apply {
                    textSize = 15f; setTextColor(0xFF888888.toInt())
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { rightMargin = 8 }
                }
                h.name = TextView(this@MainActivity).apply {
                    textSize = 16f; setTextColor(0xFF222222.toInt())
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }
                h.more = Button(this@MainActivity).apply {
                    text = "…"; textSize = 18f; setPadding(8, 0, 8, 0)
                    minimumWidth = 0; minWidth = 0
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }
                row.addView(h.cb); row.addView(h.seq); row.addView(h.name); row.addView(h.more)
                row.tag = h
            } else { row = cv; h = row.tag as VH }

            h.pos = p
            h.cb.setOnCheckedChangeListener(null)
            h.cb.isChecked = item.enabled
            h.listener = CompoundButton.OnCheckedChangeListener { _, checked ->
                if (h.pos in items.indices) { items[h.pos].enabled = checked; saveData() }
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
