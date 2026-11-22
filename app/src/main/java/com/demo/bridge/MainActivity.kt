package com.demo.bridge

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class Command(
    val name: String,
    val pkg: String,
    val action: String = ""
)

class MainActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var adapter: ArrayAdapter<String>
    private val commands = mutableListOf<Command>()
    private val notes = mutableListOf<String>()

    private val commandsFile: File
        get() = File(filesDir, "commands.json")

    private val notesFile: File
        get() = File(filesDir, "notes.json")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        listView = findViewById(R.id.listView)
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf<String>())
        listView.adapter = adapter

        loadData()
        refreshList()

        findViewById<Button>(R.id.btnAdd).setOnClickListener { showAddCommandDialog() }
        findViewById<Button>(R.id.btnNote).setOnClickListener { showAddNoteDialog() }

        listView.setOnItemClickListener { _, _, position, _ ->
            showItemMenu(position)
        }
        listView.setOnItemLongClickListener { _, _, position, _ ->
            showEditDeleteMenu(position)
            true
        }

        // Dhizuku 权限检查已移除，本节不碰系统策略
        // checkDhizukuPermission()
    }

    private fun loadData() {
        commands.clear()
        notes.clear()

        if (commandsFile.exists()) {
            try {
                val arr = JSONArray(commandsFile.readText())
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    commands.add(
                        Command(
                            name = obj.optString("name"),
                            pkg = obj.optString("pkg"),
                            action = obj.optString("action")
                        )
                    )
                }
            } catch (_: Exception) {}
        }

        if (notesFile.exists()) {
            try {
                val arr = JSONArray(notesFile.readText())
                for (i in 0 until arr.length()) {
                    notes.add(arr.getString(i))
                }
            } catch (_: Exception) {}
        }
    }

    private fun saveCommands() {
        val arr = JSONArray()
        commands.forEach { c ->
            val obj = JSONObject()
            obj.put("name", c.name)
            obj.put("pkg", c.pkg)
            obj.put("action", c.action)
            arr.put(obj)
        }
        commandsFile.writeText(arr.toString())
    }

    private fun saveNotes() {
        val arr = JSONArray()
        notes.forEach { arr.put(it) }
        notesFile.writeText(arr.toString())
    }

    private fun refreshList() {
        val items = mutableListOf<String>()
        commands.forEachIndexed { index, c ->
            items.add("CMD ${index + 1}: ${c.name} -> ${c.pkg}")
        }
        notes.forEachIndexed { index, n ->
            items.add("NOTE ${index + 1}: $n")
        }
        adapter.clear()
        adapter.addAll(items)
        adapter.notifyDataSetChanged()
    }

    private fun showAddCommandDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_command, null)
        val etName = view.findViewById<EditText>(R.id.etName)
        val etPkg = view.findViewById<EditText>(R.id.etPkg)
        val etAction = view.findViewById<EditText>(R.id.etAction)

        AlertDialog.Builder(this)
            .setTitle("添加命令")
            .setView(view)
            .setPositiveButton("保存") { _, _ ->
                val name = etName.text.toString().trim()
                val pkg = etPkg.text.toString().trim()
                val action = etAction.text.toString().trim()
                if (name.isEmpty() || pkg.isEmpty()) {
                    Toast.makeText(this, "名称和包名不能为空", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                commands.add(Command(name, pkg, action))
                saveCommands()
                refreshList()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showAddNoteDialog() {
        val et = EditText(this)
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

    private fun showItemMenu(position: Int) {
        if (position < commands.size) {
            val cmd = commands[position]
            showDeviceActionMenu(cmd.pkg)
        } else {
            val noteIndex = position - commands.size
            Toast.makeText(this, "备注：${notes[noteIndex]}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showEditDeleteMenu(position: Int) {
        val options = if (position < commands.size) {
            arrayOf("编辑命令", "删除命令")
        } else {
            arrayOf("编辑备注", "删除备注")
        }

        AlertDialog.Builder(this)
            .setTitle("操作")
            .setItems(options) { _, which ->
                if (position < commands.size) {
                    when (which) {
                        0 -> showEditCommandDialog(position)
                        1 -> {
                            commands.removeAt(position)
                            saveCommands()
                            refreshList()
                        }
                    }
                } else {
                    val idx = position - commands.size
                    when (which) {
                        0 -> showEditNoteDialog(idx)
                        1 -> {
                            notes.removeAt(idx)
                            saveNotes()
                            refreshList()
                        }
                    }
                }
            }
            .show()
    }

    private fun showEditCommandDialog(position: Int) {
        val cmd = commands[position]
        val view = layoutInflater.inflate(R.layout.dialog_command, null)
        val etName = view.findViewById<EditText>(R.id.etName)
        val etPkg = view.findViewById<EditText>(R.id.etPkg)
        val etAction = view.findViewById<EditText>(R.id.etAction)
        etName.setText(cmd.name)
        etPkg.setText(cmd.pkg)
        etAction.setText(cmd.action)

        AlertDialog.Builder(this)
            .setTitle("编辑命令")
            .setView(view)
            .setPositiveButton("保存") { _, _ ->
                commands[position] = Command(
                    etName.text.toString().trim(),
                    etPkg.text.toString().trim(),
                    etAction.text.toString().trim()
                )
                saveCommands()
                refreshList()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showEditNoteDialog(index: Int) {
        val et = EditText(this)
        et.setText(notes[index])
        AlertDialog.Builder(this)
            .setTitle("编辑备注")
            .setView(et)
            .setPositiveButton("保存") { _, _ ->
                notes[index] = et.text.toString().trim()
                saveNotes()
                refreshList()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    // 系统操作占位，本节不调用 Dhizuku / DeviceOwner
    private fun showDeviceActionMenu(pkg: String) {
        Toast.makeText(this, "系统操作暂未接入：$pkg", Toast.LENGTH_SHORT).show()
    }

    // initDpm 等系统策略逻辑本节全部移除
}
