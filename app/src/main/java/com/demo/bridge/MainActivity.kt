package com.demo.bridge

import android.annotation.SuppressLint
import android.app.Activity
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import rikka.shizuku.Shizuku

class MainActivity : Activity() {

    private lateinit var statusText: TextView
    private lateinit var adapter: ArrayAdapter<String>
    private val commands = mutableListOf<String>()
    private val notes = mutableMapOf<Int, String>()
    private var selectedIndex = -1
    private val requestCode = 1001

    private val permissionListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        if (requestCode == this.requestCode) {
            refreshStatus(grantResult == PackageManager.PERMISSION_GRANTED)
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_command)

        statusText = findViewById(R.id.statusText)
        val btnNew = findViewById<Button>(R.id.btnNew)
        val btnEdit = findViewById<Button>(R.id.btnEdit)
        val btnImportExport = findViewById<Button>(R.id.btnImportExport)
        val btnRun = findViewById<Button>(R.id.btnRun)
        val btnDelete = findViewById<Button>(R.id.btnDelete)
        val listView = findViewById<ListView>(R.id.listView)

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, displayList())
        listView.adapter = adapter

        listView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            selectedIndex = position
            Toast.makeText(this, "已选择命令 ${position + 1}", Toast.LENGTH_SHORT).show()
        }

        btnNew.setOnClickListener {
            commands.add("命令 ${commands.size + 1}")
            adapter.clear()
            adapter.addAll(displayList())
            adapter.notifyDataSetChanged()
            Toast.makeText(this, "已添加", Toast.LENGTH_SHORT).show()
        }

        btnEdit.setOnClickListener {
            if (selectedIndex !in commands.indices) {
                Toast.makeText(this, "请先选择命令", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            showNoteDialog(selectedIndex)
        }

        btnImportExport.setOnClickListener {
            Toast.makeText(this, "导入 / 导出待接入", Toast.LENGTH_SHORT).show()
        }

        btnRun.setOnClickListener {
            if (selectedIndex !in commands.indices) {
                Toast.makeText(this, "请先选择命令", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            Toast.makeText(this, "运行功能待接入", Toast.LENGTH_SHORT).show()
        }

        btnDelete.setOnClickListener {
            if (selectedIndex !in commands.indices) {
                Toast.makeText(this, "请先选择命令", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            commands.removeAt(selectedIndex)
            notes.clear()
            selectedIndex = -1
            adapter.clear()
            adapter.addAll(displayList())
            adapter.notifyDataSetChanged()
            Toast.makeText(this, "已删除", Toast.LENGTH_SHORT).show()
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
        val note = notes[index]
        if (!note.isNullOrBlank()) {
            "${index + 1}：${cmd}\n备注：$note"
        } else {
            val short = if (cmd.length > 20) cmd.substring(0, 20) + "…" else cmd
            "${index + 1}：$short"
        }
    }

    private fun showNoteDialog(index: Int) {
        val input = EditText(this)
        input.setText(notes[index] ?: "")
        AlertDialog.Builder(this)
            .setTitle("编辑备注")
            .setView(input)
            .setPositiveButton("保存") { _: DialogInterface, _: Int ->
                notes[index] = input.text.toString().trim()
                adapter.clear()
                adapter.addAll(displayList())
                adapter.notifyDataSetChanged()
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
        statusText.setTextColor(if (granted) Color.parseColor("#2E7D32") else Color.parseColor("#C62828"))
    }

    override fun onDestroy() {
        super.onDestroy()
        Shizuku.removeRequestPermissionResultListener(permissionListener)
    }
}
