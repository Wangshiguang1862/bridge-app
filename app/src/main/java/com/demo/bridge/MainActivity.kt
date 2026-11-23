package com.demo.bridge

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class MainActivity : Activity() {

    private val items = mutableListOf<CommandItem>()
    private val adapter = CommandAdapter()
    private lateinit var listView: ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        val titleBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(16, 24, 16, 16)
        }
        titleBar.addView(TextView(this@MainActivity).apply {
            text = "命令列表"
            textSize = 20f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        titleBar.addView(Button(this@MainActivity).apply {
            text = "添加"
            setOnClickListener { showAddDialog() }
        })
        titleBar.addView(Button(this@MainActivity).apply {
            text = "设置"
            setOnClickListener { startActivity(Intent(this@MainActivity, SettingsActivity::class.java)) }
        })
        titleBar.addView(Button(this@MainActivity).apply {
            text = "日志"
            setOnClickListener { startActivity(Intent(this@MainActivity, LogListActivity::class.java)) }
        })
        root.addView(titleBar)

        listView = ListView(this).apply { id = android.R.id.list }
        root.addView(listView, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ))

        setContentView(root)
        listView.adapter = adapter
        loadData()
        if (items.isEmpty()) {
            items.addAll(defaultItems())
            saveData()
            adapter.notifyDataSetChanged()
        }

        listView.setOnItemClickListener { _, _, pos, _ -> showItemMenu(items[pos], pos) }
    }

    private fun defaultItems() = listOf(
        CommandItem("打开设置", "com.android.settings", "com.android.settings.Settings", true),
        CommandItem("重启系统", "", "reboot", true),
        CommandItem("清后台", "", "am kill-all", true)
    )

    private fun loadData() {
        try {
            val f = File(applicationContext.filesDir, "commands.json")
            if (!f.exists()) return
            items.clear()
            val arr = JSONArray(f.readText())
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                items.add(CommandItem(
                    o.optString("name"), o.optString("pkg"),
                    o.optString("action"), o.optBoolean("enabled", true)
                ))
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
                put("name", it.name); put("pkg", it.pkg)
                put("action", it.action); put("enabled", it.enabled)
            })
        }
        File(applicationContext.filesDir, "commands.json").writeText(arr.toString())
    }

    private fun showAddDialog() {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(32, 16, 32, 16) }
        val etName = android.widget.EditText(this).apply { hint = "名称" }
        val etPkg = android.widget.EditText(this).apply { hint = "包名（可空）" }
        val etAction = android.widget.EditText(this).apply { hint = "动作/命令" }
        box.addView(etName); box.addView(etPkg); box.addView(etAction)
        AlertDialog.Builder(this)
            .setTitle("添加命令").setView(box)
            .setPositiveButton("保存") { _, _ ->
                items.add(CommandItem(
                    etName.text.toString(), etPkg.text.toString(),
                    etAction.text.toString(), true
                ))
                saveData(); adapter.notifyDataSetChanged()
            }
            .setNegativeButton("取消", null).show()
    }

    private fun showItemMenu(item: CommandItem, pos: Int) {
        AlertDialog.Builder(this)
            .setTitle(item.name)
            .setItems(arrayOf("运行", "删除")) { _, w ->
                when (w) {
                    0 -> {
                        val log = CommandRunner.run(this, item, true)
                        AlertDialog.Builder(this)
                            .setTitle("执行结果")
                            .setMessage("返回码: ${log.retCode}\n输出: ${log.stdout}\n错误: ${log.stderr}")
                            .setPositiveButton("确定", null).show()
                    }
                    1 -> { items.removeAt(pos); saveData(); adapter.notifyDataSetChanged() }
                }
            }.show()
    }

    data class CommandItem(val name: String, val pkg: String, val action: String, var enabled: Boolean)

    private inner class CommandAdapter : BaseAdapter() {
        override fun getCount() = items.size
        override fun getItem(p: Int) = items[p]
        override fun getItemId(p: Int) = p.toLong()
        override fun getView(p: Int, cv: View?, parent: ViewGroup?): View {
            val v = cv ?: LayoutInflater.from(this@MainActivity)
                .inflate(android.R.layout.simple_list_item_1, parent, false)
            v.findViewById<TextView>(android.R.id.text1).text = "${p + 1}. ${items[p].name}"
            return v
        }
    }
}
