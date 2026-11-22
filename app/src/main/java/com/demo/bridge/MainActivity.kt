package com.demo.bridge

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class Command(val name: String, val pkg: String, val action: String)

class MainActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var adapter: ArrayAdapter<String>
    private val commands = mutableListOf<Command>()
    private val notes = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_command)

        listView = findViewById(R.id.listView)
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf<String>())
        listView.adapter = adapter

        loadData()
        refreshList()

        findViewById<Button>(R.id.btnAdd).setOnClickListener { showAddCommand() }
        findViewById<Button>(R.id.btnNote).setOnClickListener { showAddNote() }

        listView.setOnItemClickListener { _, _, position, _ -> showItemMenu(position) }
        listView.setOnItemLongClickListener { _, _, position, _ ->
            showEditDeleteMenu(position)
            true
        }
    }

    private fun commandFile() = File(filesDir, "commands.json")
    private fun noteFile() = File(filesDir, "notes.json")

    private fun loadData() {
        commands.clear()
        notes.clear()
        try {
            val cFile = commandFile()
            if (cFile.exists()) {
                val arr = JSONArray(cFile.readText())
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    commands.add(Command(o.getString("name"), o.getString("pkg"), o.optString("action")))
                }
            }
            val nFile = noteFile()
            if (nFile.exists()) {
                val arr = JSONArray(nFile.readText())
                for (i in 0 until arr.length()) notes.add(arr.getString(i))
            }
        } catch (e: Exception) {
            Toast.makeText(this, "读取本地数据失败：${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveCommands() {
        val arr = JSONArray()
        commands.forEach {
            val o = JSONObject()
            o.put("name", it.name)
            o.put("pkg", it.pkg)
            o.put("action", it.action)
            arr.put(o)
        }
        commandFile().writeText(arr.toString())
    }

    private fun saveNotes() {
        val arr = JSONArray()
        notes.forEach { arr.put(it) }
        noteFile().writeText(arr.toString())
    }

    private fun refreshList() {
        val items = mutableListOf<String>()
        commands.forEach { items.add("命令：${it.name} | ${it.pkg}") }
        notes.forEach { items.add("备注：$it") }
        adapter.clear()
        adapter.addAll(items)
        adapter.notifyDataSetChanged()
    }

    private fun showAddCommand() {
        val intent = Intent(this, EditCommandActivity::class.java)
        startActivityForResult(intent, 1001)
    }

    private fun showAddNote() {
        val et = android.widget.EditText(this)
        AlertDialog.Builder(this)
            .setTitle("添加备注")
            .setView(et)
            .setPositiveButton("保存") { _, _ ->
                val text = et.text.toString().trim()
                if (text.isEmpty()) return@setPositiveButton
                notes.add(text)
                saveNotes()
                refreshList()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001 && resultCode == RESULT_OK && data != null) {
            val name = data.getStringExtra("name") ?: return
            val pkg = data.getStringExtra("pkg") ?: return
            val action = data.getStringExtra("action") ?: ""
            commands.add(Command(name, pkg, action))
            saveCommands()
            refreshList()
        }
    }

    private fun showItemMenu(position: Int) {
        if (position < commands.size) {
            showDeviceActionMenu(commands[position].pkg)
        } else {
            Toast.makeText(this, "备注：${notes[position - commands.size]}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showEditDeleteMenu(position: Int) {
        val isCmd = position < commands.size
        val opts = if (isCmd) arrayOf("编辑命令", "删除命令") else arrayOf("编辑备注", "删除备注")
        AlertDialog.Builder(this)
            .setTitle("操作")
            .setItems(opts) { _, which ->
                if (isCmd) {
                    when (which) {
                        0 -> {
                            val c = commands[position]
                            val i = Intent(this, EditCommandActivity::class.java).apply {
                                putExtra("name", c.name)
                                putExtra("pkg", c.pkg)
                                putExtra("action", c.action)
                                putExtra("position", position)
                            }
                            startActivityForResult(i, 1002)
                        }
                        1 -> { commands.removeAt(position); saveCommands(); refreshList() }
                    }
                } else {
                    val idx = position - commands.size
                    when (which) {
                        0 -> {
                            val et = android.widget.EditText(this)
                            et.setText(notes[idx])
                            AlertDialog.Builder(this)
                                .setTitle("编辑备注")
                                .setView(et)
                                .setPositiveButton("保存") { _, _ ->
                                    notes[idx] = et.text.toString().trim()
                                    saveNotes(); refreshList()
                                }.show()
                        }
                        1 -> { notes.removeAt(idx); saveNotes(); refreshList() }
                    }
                }
            }.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if ((requestCode == 1001 || requestCode == 1002) && resultCode == RESULT_OK && data != null) {
            val name = data.getStringExtra("name") ?: return
            val pkg = data.getStringExtra("pkg") ?: return
            val action = data.getStringExtra("action") ?: ""
            val pos = data.getIntExtra("position", -1)
            if (pos >= 0 && pos < commands.size) {
                commands[pos] = Command(name, pkg, action)
            } else {
                commands.add(Command(name, pkg, action))
            }
            saveCommands(); refreshList()
        }
    }

    // 系统操作占位：本节不接 Dhizuku，不碰 DeviceOwner
    private fun showDeviceActionMenu(pkg: String) {
        Toast.makeText(this, "系统操作暂未接入：$pkg", Toast.LENGTH_SHORT).show()
    }
}
