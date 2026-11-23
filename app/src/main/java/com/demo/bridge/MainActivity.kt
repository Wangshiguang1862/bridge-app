package com.demo.bridge

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

import rikka.shizuku.Shizuku

class MainActivity : Activity() {

    lateinit var listView: ListView
    lateinit var btnAdd: Button
    lateinit var statusText: TextView
    lateinit var tvMode: TextView
    val items = mutableListOf<CommandItem>()
    lateinit var adapter: CommandAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_command)
        Notifier.init(this)

        statusText = findViewById(R.id.statusText)
        btnAdd = findViewById(R.id.btnAdd)
        listView = findViewById(R.id.listView)
        tvMode = findViewById(R.id.tvMode)
        findViewById<Button>(R.id.btnSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        adapter = CommandAdapter()
        listView.adapter = adapter

        loadData()
        detectMode()

        btnAdd.setOnClickListener { startActivity(Intent(this, EditCommandActivity::class.java)) }
        listView.setOnItemClickListener { _, _, pos, _ -> showItemMenu(items[pos], pos) }
    }

    override fun onResume() {
        super.onResume()
        loadData()
        detectMode()
    }

    private fun detectMode() {
        GlobalState.mode = try {
            if (Shizuku.pingBinder() && Shizuku.checkSelfPermission() == 0) RunMode.SHIZUKU
            else RunMode.BASIC
        } catch (e: Exception) { RunMode.BASIC }
        tvMode.text = "运行模式：${GlobalState.mode.name.lowercase()}"
    }

    private fun loadData() {
        try {
            val f = File(filesDir, "commands.json")
            if (!f.exists()) return
            items.clear()
            val arr = JSONArray(f.readText())
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                items.add(CommandItem(o.optString("name"), o.optString("pkg"), o.optString("action"), o.optBoolean("enabled", true)))
            }
            adapter.notifyDataSetChanged()
        } catch (e: Exception) {
            Toast.makeText(this, "加载失败：${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun saveData() {
        val arr = JSONArray()
        for (it in items) arr.put(JSONObject().apply {
            put("name", it.name); put("pkg", it.pkg); put("action", it.action); put("enabled", it.enabled)
        })
        File(filesDir, "commands.json").writeText(arr.toString())
    }

    private fun showItemMenu(item: CommandItem, pos: Int) {
        AlertDialog.Builder(this).setTitle(item.name).setItems(arrayOf("编辑", "导入/导出", "运行", "删除")) { _, w ->
            when (w) {
                0 -> startActivity(Intent(this, EditCommandActivity::class.java).apply {
                    putExtra("name", item.name); putExtra("pkg", item.pkg); putExtra("action", item.action); putExtra("enabled", item.enabled)
                })
                1 -> Toast.makeText(this, "导入/导出：后续版本", Toast.LENGTH_SHORT).show()
                2 -> {
                    val log = CommandRunner.run(this, item, true)
                    AlertDialog.Builder(this).setTitle("执行结果").setMessage(
                        "返回码：${log.retCode}\n输出：${log.stdout}\n错误：${log.stderr}"
                    ).setPositiveButton("确定", null).show()
                }
                3 -> { items.removeAt(pos); saveData(); adapter.notifyDataSetChanged() }
            }
        }.show()
    }

    data class CommandItem(val name: String, val pkg: String, val action: String, var enabled: Boolean)

    private inner class CommandAdapter : BaseAdapter() {
        override fun getCount() = items.size
        override fun getItem(p: Int) = items[p]
        override fun getItemId(p: Int) = p.toLong()
        override fun getView(p: Int, cv: View?, parent: ViewGroup?): View {
            val v = cv ?: LayoutInflater.from(this@MainActivity).inflate(R.layout.item_command, parent, false)
            val it = items[p]
            v.findViewById<TextView>(R.id.tvSeq).text = (p + 1).toString()
            v.findViewById<TextView>(R.id.tvName).text = it.name
            v.findViewById<CheckBox>(R.id.cbEnable).apply {
                isChecked = it.enabled
                setOnCheckedChangeListener { _, c -> it.enabled = c; saveData() }
            }
            return v
        }
    }
}
