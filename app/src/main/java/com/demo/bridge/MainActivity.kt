package com.demo.bridge

import android.app.AlertDialog
import android.os.Bundle
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class Command(val name: String, val pkg: String, val action: String)

class MainActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private val commands = mutableListOf<Command>()
    private val notes = mutableListOf<String>()
    private lateinit var adapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_command)

        listView = findViewById(R.id.listView)
        adapter = ArrayAdapter(this, R.layout.item_command, R.id.text, mutableListOf())
        listView.adapter = adapter

        loadData()

        findViewById<Button>(R.id.btnAdd).setOnClickListener { showAddCommandDialog() }
        findViewById<Button>(R.id.btnNote).setOnClickListener { showAddNoteDialog() }

        listView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            showItemMenu(position)
        }
        listView.onItemLongClickListener = AdapterView.OnItemLongClickListener { _, _, position, _ ->
            showEditDeleteMenu(position)
            true
        }

        // Shizuku 只做检测提示，不强制初始化
        checkShizukuStatus()
    }

    private fun checkShizukuStatus() {
        Toast.makeText(this, "Shizuku 状态检测暂留空，系统策略未接入", Toast.LENGTH_SHORT).show()
    }

    private fun file(name: String) = File(filesDir, name)

    private fun loadData() {
        runCatching {
            val cf = file("commands.json")
            if (cf.exists()) {
                val arr = JSONArray(cf.readText())
                commands.clear()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    commands.add(Command(o.getString("name"), o.getString("pkg"), o.optString("action")))
                }
            }
            val nf = file("notes.json")
            if (nf.exists()) {
                val arr = JSONArray(nf.readText())
                notes.clear()
                for (i in 0 until arr.length()) notes.add(arr.getString(i))
            }
        }.onFailure { it.printStackTrace() }
        refreshList()
    }

    private fun saveCommands() {
        val arr = JSONArray()
        commands.forEach {
            arr.put(JSONObject().apply {
                put("name", it.name); put("pkg", it.pkg); put("action", it.action)
            })
        }
        file("commands.json").writeText(arr.toString())
    }

    private fun saveNotes() {
        val arr = JSONArray()
        notes.forEach { arr.put(it) }
        file("notes.json").writeText(arr.toString())
    }

    private fun refreshList() {
        val data = commands.map { "${it.name}  ${it.pkg}" } + notes.map { "备注：$it" }
        adapter.clear()
        adapter.addAll(data)
        adapter.notifyDataSetChanged()
    }

    private fun showAddCommandDialog() {
        val v = layoutInflater.inflate(R.layout.activity_edit_command, null)
        val etName = v.findViewById<EditText>(R.id.etName)
        val etPkg = v.findViewById<EditText>(R.id.etPkg)
        val etAction = v.findViewById<EditText>(R.id.etAction)
        AlertDialog.Builder(this)
            .setTitle("添加命令")
            .setView(v)
            .setPositiveButton("保存") { _, _ ->
                val name = etName.text.toString().trim()
                val pkg = etPkg.text.toString().trim()
                if (name.isEmpty() || pkg.isEmpty()) {
                    Toast.makeText(this, "名称和包名不能为空", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                commands.add(Command(name, pkg, etAction.text.toString().trim()))
                saveCommands(); refreshList()
            }
            .setNegativeButton("取消", null).show()
    }

    private fun showAddNoteDialog() {
        val et = EditText(this)
        AlertDialog.Builder(this)
            .setTitle("添加备注")
            .setView(et)
            .setPositiveButton("保存") { _, _ ->
                val t = et.text.toString().trim()
                if (t.isNotEmpty()) { notes.add(t); saveNotes(); refreshList() }
            }
            .setNegativeButton("取消", null).show()
    }

    private fun showItemMenu(position: Int) {
        if (position < commands.size) showDeviceActionMenu(commands[position].pkg)
        else Toast.makeText(this, "备注：${notes[position - commands.size]}", Toast.LENGTH_SHORT).show()
    }

    private fun showEditDeleteMenu(position: Int) {
        val opts = if (position < commands.size) arrayOf("编辑命令", "删除命令") else arrayOf("编辑备注", "删除备注")
        AlertDialog.Builder(this).setTitle("操作").setItems(opts) { _, w ->
            if (position < commands.size) {
                when (w) { 0 -> showEditCommandDialog(position); 1 -> { commands.removeAt(position); saveCommands(); refreshList() } }
            } else {
                val i = position - commands.size
                when (w) { 0 -> showEditNoteDialog(i); 1 -> { notes.removeAt(i); saveNotes(); refreshList() } }
            }
        }.show()
    }

    private fun showEditCommandDialog(pos: Int) {
        val c = commands[pos]
        val v = layoutInflater.inflate(R.layout.activity_edit_command, null)
        val etName = v.findViewById<EditText>(R.id.etName)
        val etPkg = v.findViewById<EditText>(R.id.etPkg)
        val etAction = v.findViewById<EditText>(R.id.etAction)
        etName.setText(c.name); etPkg.setText(c.pkg); etAction.setText(c.action)
        AlertDialog.Builder(this)
            .setTitle("编辑命令")
            .setView(v)
            .setPositiveButton("保存") { _, _ ->
                commands[pos] = Command(etName.text.toString().trim(), etPkg.text.toString().trim(), etAction.text.toString().trim())
                saveCommands(); refreshList()
            }
            .setNegativeButton("取消", null).show()
    }

    private fun showEditNoteDialog(i: Int) {
        val et = EditText(this); et.setText(notes[i])
        AlertDialog.Builder(this)
            .setTitle("编辑备注").setView(et)
            .setPositiveButton("保存") { _, _ -> notes[i] = et.text.toString().trim(); saveNotes(); refreshList() }
            .setNegativeButton("取消", null).show()
    }

    // 系统操作占位，本节不接 Dhizuku / DeviceOwner
    private fun showDeviceActionMenu(pkg: String) {
        Toast.makeText(this, "系统操作暂未接入：$pkg", Toast.LENGTH_SHORT).show()
    }
}
