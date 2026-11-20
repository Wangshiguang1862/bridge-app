package com.demo.bridge

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class CommandActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var adapter: CommandAdapter
    private val commands = mutableListOf<CommandItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_command)

        listView = findViewById(R.id.listView)
        adapter = CommandAdapter(this, commands)
        listView.adapter = adapter

        refreshList()
    }

    private fun refreshList() {
        val dir = File(filesDir, "commands")
        if (!dir.exists()) dir.mkdirs()
        commands.clear()
        dir.listFiles { f -> f.isFile && f.name.endsWith(".sh") }
            ?.sortedBy { it.name }
            ?.forEachIndexed { idx, file ->
                commands.add(CommandItem(idx + 1, file))
            }
        adapter.notifyDataSetChanged()
    }

    fun addCommand(view: View) {
        startActivityForResult(Intent(this, EditCommandActivity::class.java), 100)
    }

    fun importCommand(view: View) {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        startActivityForResult(Intent.createChooser(intent, "导入命令"), 101)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            refreshList()
        }
    }
}

