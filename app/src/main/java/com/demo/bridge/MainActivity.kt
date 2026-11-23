package com.demo.bridge

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class CommandItem(
    var id: Long = System.currentTimeMillis(),
    var name: String = "",
    var pkg: String = "",
    var action: String = "",
    var note: String = ""
)

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var listView: ListView
    private lateinit var btnAdd: ImageButton
    private lateinit var adapter: CommandAdapter
    private val items = mutableListOf<CommandItem>()

    private val dataFile: File
        get() = File(filesDir, "commands.json")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_command)

        statusText = findViewById(R.id.statusText)
        listView = findViewById(R.id.listView)
        btnAdd = findViewById(R.id.btnAdd)

        adapter = CommandAdapter()
        listView.adapter = adapter

        loadData()

        btnAdd.setOnClickListener { showEditDialog(null) }

        listView.onItemClickListener = AdapterView.OnItemClickListener { _, _, pos, _ ->
            showEditDialog(items[pos])
        }
        listView.onItemLongClickListener = AdapterView.OnItemLongClickListener { _, _, pos, _ ->
            showItemMenu(items[pos], pos)
            true
        }
    }

    private fun loadData() {
        items.clear()
        if (!dataFile.exists()) {
            refreshList()
            return
        }
        try {
            val arr = JSONArray(dataFile.readText())
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                items.add(
                    CommandItem(
                        id = o.optLong("id"),
                        name = o.optString("name"),
                        pkg = o.optString("pkg"),
                        action = o.optString("action"),
                        note = o.optString("note")
                    )
                )
            }
        } catch (_: Exception) {}
        refreshList()
    }

    private fun saveData() {
        val arr = JSONArray()
        items.forEach {
            val o = JSONObject()
            o.put("id", it.id)
            o.put("name", it.name)
            o.put("pkg", it.pkg)
            o.put("action", it.action)
            o.put("note", it.note)
            arr.put(o)
        }
        dataFile.writeText(arr.toString())
    }

    private fun refreshList() {
        adapter.notifyDataSetChanged()
    }

    private fun showEditDialog(item: CommandItem?) {
        val view = LayoutInflater.from(this).inflate(R.layout.activity_edit_command, null)
        val etContent = view.findViewById<EditText>(R.id.etContent)

        if (item != null) {
            etContent.setText(buildString {
                append(item.name)
                append('\n').append(item.pkg)
                append('\n').append(item.action)
            })
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle(if (item == null) "添加命令" else "编辑命令")
            .setView(view)
            .setNegativeButton("取消", null)
            .create()

        view.findViewById<android.widget.Button>(R.id.btnSave).setOnClickListener {
            val text = etContent.text.toString().trim()
            if (text.isEmpty()) {
                Toast.makeText(this, "内容不能为空", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val lines = text.lines().map { it.trim() }
            val name = lines.getOrNull(0) ?: ""
            val pkg = lines.getOrNull(1) ?: ""
            val action = lines.getOrNull(2) ?: ""

            if (item == null) {
                items.add(CommandItem(name = name, pkg = pkg, action = action))
            } else {
                item.name = name
                item.pkg = pkg
                item.action = action
            }
            saveData()
            refreshList()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showItemMenu(item: CommandItem, pos: Int) {
        val opts = arrayOf("编辑", "删除", "系统操作")
        AlertDialog.Builder(this@MainActivity)
            .setTitle(item.name)
            .setItems(opts) { _, which ->
                when (which) {
                    0 -> showEditDialog(item)
                    1 -> {
                        items.removeAt(pos)
                        saveData()
                        refreshList()
                    }
                    2 -> showDeviceActionMenu(item.pkg)
                }
            }
            .show()
    }

    private fun showDeviceActionMenu(pkg: String) {
        Toast.makeText(this@MainActivity, "系统操作暂未接入：$pkg", Toast.LENGTH_SHORT).show()
    }

    private inner class CommandAdapter : BaseAdapter() {
        override fun getCount(): Int = items.size
        override fun getItem(position: Int): Any = items[position]
        override fun getItemId(position: Int): Long = items[position].id

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val inflater = LayoutInflater.from(this@MainActivity)
            val v = convertView ?: inflater.inflate(R.layout.item_command, parent, false)
            val tv = v.findViewById<TextView>(R.id.text)
            val it = items[position]
            tv.text = if (it.note.isNotEmpty()) {
                it.note
            } else {
                buildString {
                    append(it.name)
                    if (it.pkg.isNotEmpty()) append('\n').append(it.pkg)
                    if (it.action.isNotEmpty()) append('\n').append(it.action)
                }
            }
            return v
        }
    }
}
