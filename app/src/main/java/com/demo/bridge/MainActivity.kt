package com.demo.bridge

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.EditText
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

    private lateinit var listView: ListView
    private lateinit var adapter: CommandAdapter
    private val items = mutableListOf<CommandItem>()

    private val dataFile: File
        get() = File(filesDir, "commands.json")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_command)

        listView = findViewById(R.id.listView)
        adapter = CommandAdapter()
        listView.adapter = adapter

        loadData()

        findViewById<Button>(R.id.btnAdd).setOnClickListener { showEditDialog(null) }
        findViewById<Button>(R.id.btnNote).setOnClickListener { showAddNoteDialog() }

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
        val etName = view.findViewById<EditText>(R.id.etName)
        val etPkg = view.findViewById<EditText>(R.id.etPkg)
        val etAction = view.findViewById<EditText>(R.id.etAction)

        if (item != null) {
            etName.setText(item.name)
            etPkg.setText(item.pkg)
            etAction.setText(item.action)
        }

        AlertDialog.Builder(this)
            .setTitle(if (item == null) "添加命令" else "编辑命令")
            .setView(view)
            .setPositiveButton("保存") { _, _ ->
                val name = etName.text.toString().trim()
                val pkg = etPkg.text.toString().trim()
                val action = etAction.text.toString().trim()
                if (name.isEmpty() || pkg.isEmpty()) {
                    Toast.makeText(this, "名称和包名不能为空", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (item == null) {
                    items.add(CommandItem(name = name, pkg = pkg, action = action))
                } else {
                    item.name = name
                    item.pkg = pkg
                    item.action = action
                }
                saveData()
                refreshList()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showAddNoteDialog() {
        val et = EditText(this)
        et.hint = "备注内容"
        AlertDialog.Builder(this)
            .setTitle("添加备注")
            .setView(et)
            .setPositiveButton("保存") { _, _ ->
                val text = et.text.toString().trim()
                if (text.isEmpty()) return@setPositiveButton
                items.add(CommandItem(name = "备注", pkg = "", note = text))
                saveData()
                refreshList()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showItemMenu(item: CommandItem, pos: Int) {
        val opts = arrayOf("编辑", "删除", "系统操作")
        AlertDialog.Builder(this)
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
        Toast.makeText(this, "系统操作暂未接入：$pkg", Toast.LENGTH_SHORT).show()
    }

    private inner class CommandAdapter : BaseAdapter() {
        override fun getCount(): Int = items.size
        override fun getItem(position: Int): Any = items[position]
        override fun getItemId(position: Int): Long = items[position].id

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val v = convertView ?: LayoutInflater.from(context)
                .inflate(R.layout.item_command, parent, false)
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
