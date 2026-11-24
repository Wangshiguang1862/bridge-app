package com.demo.bridge

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject

class ExportSelectActivity : Activity() {

    private val selected = mutableSetOf<Int>()
    private lateinit var items: List<MainActivity.CommandItem>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        items = BridgeData.loadItems(this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
        }

        val cbAll = CheckBox(this).apply {
            text = "全选"
        }
        root.addView(cbAll)

        val listView = ListView(this)
        val adapter = object : ArrayAdapter<MainActivity.CommandItem>(
            this, android.R.layout.simple_list_item_multiple_choice, items
        ) {
            override fun getView(position: Int, convertView: android.view.View?, parent: android.view.ViewGroup): android.view.View {
                val v = super.getView(position, convertView, parent)
                (v as CheckedTextView).text = items[position].name
                return v
            }
        }
        listView.adapter = adapter
        listView.choiceMode = ListView.CHOICE_MODE_MULTIPLE
        root.addView(listView, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ))

        cbAll.setOnCheckedChangeListener { _, checked ->
            selected.clear()
            for (i in items.indices) {
                listView.setItemChecked(i, checked)
                if (checked) selected.add(i)
            }
        }

        listView.setOnItemClickListener { _, _, pos, _ ->
            if (pos in selected) selected.remove(pos) else selected.add(pos)
            cbAll.isChecked = selected.size == items.size
        }

        val btnOk = Button(this).apply {
            text = "确定导出"
            setOnClickListener {
                if (selected.isEmpty()) {
                    Toast.makeText(this@ExportSelectActivity, "请至少选择一条", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val arr = JSONArray()
                selected.forEach { idx ->
                    val it = items[idx]
                    val o = JSONObject()
                    o.put("name", it.name)
                    o.put("pkg", it.pkg)
                    o.put("action", it.action)
                    o.put("enabled", it.enabled)
                    arr.put(o)
                }
                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "application/json"
                    putExtra(Intent.EXTRA_TITLE, "bridge_export_selected.json")
                }
                // 把 JSON 字符串暂存，等回调再写
                getSharedPreferences("export_temp", MODE_PRIVATE)
                    .edit().putString("json", arr.toString()).apply()
                startActivityForResult(intent, 2001)
            }
        }
        root.addView(btnOk)

        setContentView(root)
    }

    override fun onActivityResult(req: Int, res: Int, data: Intent?) {
        super.onActivityResult(req, res, data)
        if (req == 2001 && res == RESULT_OK && data != null) {
            val json = getSharedPreferences("export_temp", MODE_PRIVATE)
                .getString("json", "") ?: ""
            data.data?.let { uri ->
                contentResolver.openOutputStream(uri)?.use {
                    it.write(json.toByteArray())
                }
                Toast.makeText(this, "导出成功 ${selected.size} 条", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}
