package com.demo.bridge

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
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

        // 兜底布局，避免依赖不存在的 activity_main.xml
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(TextView(this@MainActivity).apply { text = "命令列表" })
        }
        listView = ListView(this).apply { id = android.R.id.list }
        root.addView(
            listView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )
        setContentView(root)

        listView.adapter = adapter
        loadData()

        listView.setOnItemClickListener { _, _, pos, _ ->
            showItemMenu(items[pos], pos)
        }
    }

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
                        name = o.optString("name"),
                        pkg = o.optString("pkg"),
                        action = o.optString("action"),
                        enabled = o.optBoolean("enabled", true)
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
            arr.put(
                JSONObject().apply {
                    put("name", it.name)
                    put("pkg", it.pkg)
                    put("action", it.action)
                    put("enabled", it.enabled)
                }
            )
        }
        File(applicationContext.filesDir, "commands.json").writeText(arr.toString())
    }

    private fun showItemMenu(item: CommandItem, pos: Int) {
        AlertDialog.Builder(this)
            .setTitle(item.name)
            .setItems(arrayOf("编辑", "导入/导出", "运行", "删除")) { _, w ->
                when (w) {
                    0 -> startActivity(
                        Intent(this, EditCommandActivity::class.java).apply {
                            putExtra("name", item.name)
                            putExtra("pkg", item.pkg)
                            putExtra("action", item.action)
                        }
                    )
                    1 -> Toast.makeText(
                        this,
                        "导入/导出：后续版本",
                        Toast.LENGTH_SHORT
                    ).show()
                    2 -> {
                        val log = CommandRunner.run(this, item, true)
                        AlertDialog.Builder(this)
                            .setTitle("执行结果")
                            .setMessage(
                                "返回码: ${log.retCode}\n输出: ${log.stdout}\n错误: ${log.stderr}"
                            )
                            .setPositiveButton("确定", null)
                            .show()
                    }
                    3 -> {
                        items.removeAt(pos)
                        saveData()
                        adapter.notifyDataSetChanged()
                    }
                }
            }
            .show()
    }

    data class CommandItem(
        val name: String,
        val pkg: String,
        val action: String,
        var enabled: Boolean
    )

    private inner class CommandAdapter : BaseAdapter() {
        override fun getCount() = items.size
        override fun getItem(p: Int) = items[p]
        override fun getItemId(p: Int) = p.toLong()

        override fun getView(p: Int, cv: View?, parent: ViewGroup?): View {
            val v = cv ?: LayoutInflater.from(this@MainActivity)
                .inflate(android.R.layout.simple_list_item_1, parent, false)
            val it = items[p]
            v.findViewById<TextView>(android.R.id.text1).text = "${p + 1}. ${it.name}"
            return v
        }
    }
}
