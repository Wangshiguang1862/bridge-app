package com.demo.bridge

import android.Manifest
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import rikka.shizuku.Shizuku

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var listView: ListView
    private lateinit var adapter: CommandAdapter

    private val commands = mutableListOf<String>()
    private val notes = mutableMapOf<String, String>()
    private val enabled = mutableMapOf<String, Boolean>()

    private var selectedIndex = -1
    private val requestCode = 1001

    private val permissionListener = Shizuku.OnRequestPermissionResultListener { _, grantResult ->
        val granted = grantResult == PackageManager.PERMISSION_GRANTED
        refreshStatus(granted)
        if (!granted) Toast.makeText(this, "Shizuku 未授权", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_command)

        statusText = findViewById(R.id.statusText)
        listView = findViewById(R.id.listView)
        val btnAdd = findViewById<ImageButton>(R.id.btnAdd)

        loadState()
        adapter = CommandAdapter()
        listView.adapter = adapter

        btnAdd.setOnClickListener {
            val cmd = "命令 ${commands.size + 1}"
            commands.add(cmd)
            notes[cmd] = ""
            enabled[cmd] = true
            adapter.notifyDataSetChanged()
            persistState()
            Toast.makeText(this, "已添加", Toast.LENGTH_SHORT).show()
        }

        Shizuku.addRequestPermissionResultListener(permissionListener)

        if (Shizuku.isPreV11() || Shizuku.getVersion() < 11) {
            setStatus(false, "Shizuku 版本过低")
        } else if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            setStatus(false, "未授权")
            Shizuku.requestPermission(requestCode)
        } else {
            setStatus(true, "已授权")
        }
    }

    private fun displayList(): List<String> = commands.mapIndexed { index, cmd ->
        val note = notes[cmd]
        if (!note.isNullOrBlank()) {
            "${index + 1}：${cmd}\n备注：$note"
        } else {
            val short = if (cmd.length > 20) cmd.substring(0, 20) + "…" else cmd
            "${index + 1}：$short"
        }
    }

    private fun loadState() {
        val prefs = getSharedPreferences("bridge", MODE_PRIVATE)
        commands.clear()
        prefs.getStringSet("commands", emptySet())?.let { commands.addAll(it) }
        notes.clear()
        prefs.getStringSet("notes", emptySet())?.forEach {
            val parts = it.split("\u0001", limit = 2)
            if (parts.size == 2) notes[parts[0]] = parts[1]
        }
        enabled.clear()
        prefs.getStringSet("enabled", emptySet())?.forEach {
            val parts = it.split("\u0001", limit = 2)
            if (parts.size == 2) enabled[parts[0]] = parts[1].toBoolean()
        }
    }

    private fun persistState() {
        getSharedPreferences("bridge", MODE_PRIVATE).edit().apply {
            putStringSet("commands", commands.toSet())
            putStringSet("notes", notes.map { "${it.key}\u0001${it.value}" }.toSet())
            putStringSet("enabled", enabled.map { "${it.key}\u0001${it.value}" }.toSet())
            apply()
        }
    }

    private fun showActionMenu(position: Int) {
        if (position !in commands.indices) return
        selectedIndex = position
        val cmd = commands[position]
        val items = arrayOf("编辑", "导入导出", "运行", "删除")
        AlertDialog.Builder(this)
            .setTitle("命令 ${position + 1}")
            .setItems(items) { _: DialogInterface, which: Int ->
                when (which) {
                    0 -> showNoteDialog(cmd)
                    1 -> Toast.makeText(this, "导入 / 导出待接入", Toast.LENGTH_SHORT).show()
                    2 -> Toast.makeText(this, "运行功能待接入", Toast.LENGTH_SHORT).show()
                    3 -> {
                        commands.removeAt(position)
                        notes.remove(cmd)
                        enabled.remove(cmd)
                        selectedIndex = -1
                        adapter.notifyDataSetChanged()
                        persistState()
                        Toast.makeText(this, "已删除", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showNoteDialog(cmd: String) {
        val input = EditText(this)
        input.setText(notes[cmd] ?: "")
        AlertDialog.Builder(this)
            .setTitle("编辑备注")
            .setView(input)
            .setPositiveButton("保存") { _: DialogInterface, _: Int ->
                notes[cmd] = input.text.toString().trim()
                adapter.notifyDataSetChanged()
                persistState()
            }
            .setNegativeButton("取消") { dialog: DialogInterface, _: Int ->
                dialog.dismiss()
            }
            .show()
    }

    private fun refreshStatus(granted: Boolean) {
        setStatus(granted, if (granted) "已授权" else "未授权")
    }

    private fun setStatus(granted: Boolean, text: String) {
        statusText.text = text
        statusText.setTextColor(
            if (granted) Color.parseColor("#2E7D32") else Color.parseColor("#C62828")
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        Shizuku.removeRequestPermissionResultListener(permissionListener)
    }

    inner class CommandAdapter : BaseAdapter() {
        private val inflater = LayoutInflater.from(this@MainActivity)

        override fun getCount() = commands.size
        override fun getItem(position: Int) = displayList()[position]
        override fun getItemId(position: Int) = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: inflater.inflate(R.layout.item_command, parent, false)
            val tvSeq = view.findViewById<TextView>(R.id.tvSeq)
            val tvName = view.findViewById<TextView>(R.id.tvName)
            val cbEnable = view.findViewById<CheckBox>(R.id.cbEnable)
            val more = view.findViewById<ImageButton>(R.id.itemMore)

            val cmd = commands[position]
            tvSeq.text = (position + 1).toString()
            tvName.text = (notes[cmd]?.takeIf { it.isNotBlank() } ?: cmd).take(20)

            cbEnable.setOnCheckedChangeListener(null)
            cbEnable.isChecked = enabled[cmd] != false
            cbEnable.setOnCheckedChangeListener { _, isChecked ->
                enabled[cmd] = isChecked
                persistState()
            }

            view.setOnClickListener { showActionMenu(position) }
            more.setOnClickListener { showActionMenu(position) }
            return view
        }
    }
}
