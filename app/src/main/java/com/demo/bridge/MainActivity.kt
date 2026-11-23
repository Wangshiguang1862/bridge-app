package com.demo.bridge

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.json.JSONArray
import org.json.JSONObject
import rikka.shizuku.Shizuku

class MainActivity : AppCompatActivity() {

    companion object {
        const val REQ_EDIT = 1001
        const val REQ_SHIZUKU = 2001
        const val REQ_NOTIFY = 3001
        const val PREFS = "bridge_prefs"
        const val KEY_DATA = "commands_json"
    }

    private lateinit var listView: ListView
    private lateinit var adapter: CommandAdapter
    private lateinit var tvMode: TextView
    val items = mutableListOf<CommandItem>()

    data class CommandItem(
        val name: String,
        val pkg: String,
        val action: String,
        var enabled: Boolean
    )

    private class VH {
        lateinit var cb: CheckBox
        lateinit var seq: TextView
        lateinit var name: TextView
        lateinit var more: Button
        var pos: Int = 0
        var listener: CompoundButton.OnCheckedChangeListener? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadData()
        checkNotifyPermission()
        buildUi()
        detectMode()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFFF5F5F5.toInt())
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(24, 28, 24, 24)
            setBackgroundColor(0xFFFFFFFF.toInt())
        }
        val title = TextView(this).apply {
            text = "Bridge"
            textSize = 24f
            setTextColor(0xFF111111.toInt())
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        tvMode = TextView(this).apply {
            textSize = 13f
            setTextColor(0xFF666666.toInt())
            setPadding(12, 0, 0, 0)
        }
        val btnSetting = Button(this).apply {
            text = "设置"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { weight = 1f; gravity = android.view.Gravity.END }
            setOnClickListener { startActivity(Intent(this@MainActivity, SettingsActivity::class.java)) }
        }
        header.addView(title)
        header.addView(tvMode)
        header.addView(btnSetting)
        root.addView(header)

        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(24, 16, 24, 16)
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
            textSize = 16f
            setTextColor(0xFF222222.toInt())
            setPadding(16, 0, 0, 0)
        }
        bar.addView(btnAdd)
        bar.addView(tvList)
        root.addView(bar)

        listView = ListView(this).apply { dividerHeight = 1 }
        adapter = CommandAdapter()
        listView.adapter = adapter
        root.addView(listView, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ))
        setContentView(root)
    }

    private fun detectMode() {
        when {
            Shizuku.pingBinder() -> {
                if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                    tvMode.text = "● 运行模式：shizuku"
                } else {
                    tvMode.text = "○ 运行模式：shizuku（待授权）"
                    requestShizuku()
                }
            }
            else -> tvMode.text = "○ 运行模式：基础"
        }
    }

    private fun requestShizuku() {
        if (Shizuku.shouldShowRequestPermissionRationale()) {
            Toast.makeText(this, "请授予 Bridge 的 Shizuku 权限", Toast.LENGTH_LONG).show()
        }
        Shizuku.requestPermission(REQ_SHIZUKU)
    }

    override fun onRequestPermissionsResult(code: Int, perms: Array<String>, grant: IntArray) {
        super.onRequestPermissionsResult(code, perms, grant)
        if (code == REQ_SHIZUKU || code == REQ_NOTIFY) detectMode()
    }

    private fun checkNotifyPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQ_NOTIFY)
        }
    }

    private fun loadData() {
        items.clear()
        try {
            val raw = getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_DATA, "[]") ?: "[]"
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                items.add(CommandItem(
                    o.optString("name"),
                    o.optString("pkg"),
                    o.optString("action"),
                    o.optBoolean("enabled", false)
                ))
            }
        } catch (_: Exception) {}
    }

    fun saveData() {
        val arr = JSONArray()
        items.forEach {
            val o = JSONObject()
            o.put("name", it.name)
            o.put("pkg", it.pkg)
            o.put("action", it.action)
            o.put("enabled", it.enabled)
            arr.put(o)
        }
        getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_DATA, arr.toString()).apply()
    }

    private fun showItemMenu(item: CommandItem, pos: Int) {
        AlertDialog.Builder(this)
            .setTitle(item.name)
            .setItems(arrayOf("编辑", "导入/导出", "立即执行", "删除")) { _, w ->
                when (w) {
                    0 -> startActivityForResult(Intent(this, EditCommandActivity::class.java).apply {
                        putExtra("isEdit", true)
                        putExtra("originalName", item.name)
                        putExtra("name", item.name)
                        putExtra("pkg", item.pkg)
                        putExtra("action", item.action)
                    }, REQ_EDIT)
                    1 -> Toast.makeText(this, "导入/导出：后续版本", Toast.LENGTH_SHORT).show()
                    2 -> Toast.makeText(this, "执行：${item.name}", Toast.LENGTH_SHORT).show()
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
            } else {
                items.add(CommandItem(name, pkg, action, false))
            }
            saveData()
            adapter.notifyDataSetChanged()
        }
    }

    private inner class CommandAdapter : BaseAdapter() {
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
                    gravity = android.view.Gravity.CENTER_VERTICAL
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
            h.listener = null
            h.cb.setOnCheckedChangeListener(null)
            h.cb.isChecked = item.enabled
            h.listener = CompoundButton.OnCheckedChangeListener { _, checked ->
                if (h.pos in items.indices) {
                    items[h.pos].enabled = checked
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
