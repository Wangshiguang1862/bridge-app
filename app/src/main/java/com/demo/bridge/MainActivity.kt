package com.demo.bridge

import android.Manifest
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import dev.rikka.shizuku.Shizuku
import io.github.iamros.dhizuku.Dhizuku
import io.github.iamros.dhizuku.DhizukuRequestPermissionListener
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: CommandAdapter
    private lateinit var inputCommand: TextInputEditText
    private lateinit var inputDesc: TextInputEditText
    private lateinit var btnAdd: MaterialButton
    private lateinit var btnRun: MaterialButton

    private val commands = mutableListOf<Command>()
    private val notes = mutableListOf<String>()

    private lateinit var dpm: android.app.admin.DevicePolicyManager
    private lateinit var admin: ComponentName

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                Toast.makeText(this, "需要存储权限才能读写命令文件", Toast.LENGTH_SHORT).show()
            }
        }

    private val dhizukuListener = object : DhizukuRequestPermissionListener() {
        override fun onRequestPermission(grantResult: Int) {
            if (grantResult == PackageManager.PERMISSION_GRANTED) {
                initDpm()
                Toast.makeText(this@MainActivity, "Dhizuku 授权成功", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@MainActivity, "Dhizuku 未授权", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerView)
        inputCommand = findViewById(R.id.inputCommand)
        inputDesc = findViewById(R.id.inputDesc)
        btnAdd = findViewById(R.id.btnAdd)
        btnRun = findViewById(R.id.btnRun)

        adapter = CommandAdapter(commands, { pos -> showRunMenu(pos) }, { pos -> showEditMenu(pos) })
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        loadState()

        btnAdd.setOnClickListener { addCommand() }

        checkPermission()
        checkShizukuPermission()
        checkDhizukuPermission()
    }

    private fun checkPermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        }
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(permission)
        }
    }

    private fun checkShizukuPermission() {
        if (!Shizuku.pingBinder()) return
        if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            Shizuku.requestPermission(1001)
        }
    }

    private fun checkDhizukuPermission() {
        if (!Dhizuku.isAvailable()) {
            Toast.makeText(this, "未检测到 Dhizuku", Toast.LENGTH_SHORT).show()
            return
        }
        if (!Dhizuku.hasPermission()) {
            Dhizuku.requestPermission(dhizukuListener)
        } else {
            initDpm()
        }
    }

    private fun initDpm() {
        admin = ComponentName(this, Dhizuku.getDhizukuReceiver())
        dpm = Dhizuku.getDevicePolicyManager()
    }

    private fun addCommand() {
        val cmd = inputCommand.text?.toString()?.trim().orEmpty()
        val desc = inputDesc.text?.toString()?.trim().orEmpty()
        if (cmd.isEmpty()) {
            Toast.makeText(this, "命令不能为空", Toast.LENGTH_SHORT).show()
            return
        }
        commands.add(Command(cmd, desc))
        inputCommand.text?.clear()
        inputDesc.text?.clear()
        adapter.notifyItemInserted(commands.size - 1)
        persistState()
    }

    private fun showRunMenu(pos: Int) {
        val items = arrayOf("运行", "设备操作")
        AlertDialog.Builder(this)
            .setTitle(commands[pos].command)
            .setItems(items) { _, which ->
                when (which) {
                    0 -> runCommand(pos)
                    1 -> showDeviceActionMenu(commands[pos].command)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showEditMenu(pos: Int) {
        val items = arrayOf("编辑", "删除")
        AlertDialog.Builder(this)
            .setTitle(commands[pos].command)
            .setItems(items) { _, which ->
                when (which) {
                    0 -> editCommand(pos)
                    1 -> deleteCommand(pos)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun runCommand(pos: Int) {
        val cmd = commands[pos].command
        try {
            val proc = Runtime.getRuntime().exec(arrayOf("sh", "-c", cmd))
            val out = proc.inputStream.bufferedReader().readText()
            val err = proc.errorStream.bufferedReader().readText()
            val res = if (err.isNotBlank()) err else out
            AlertDialog.Builder(this)
                .setTitle("执行结果")
                .setMessage(res.ifBlank { "无输出" })
                .setPositiveButton("关闭", null)
                .show()
        } catch (e: Exception) {
            Toast.makeText(this, "执行失败：${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun editCommand(pos: Int) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit, null)
        val etCmd = dialogView.findViewById<TextInputEditText>(R.id.editCommand)
        val etDesc = dialogView.findViewById<TextInputEditText>(R.id.editDesc)
        etCmd.setText(commands[pos].command)
        etDesc.setText(commands[pos].desc)
        AlertDialog.Builder(this)
            .setTitle("编辑命令")
            .setView(dialogView)
            .setPositiveButton("保存") { _, _ ->
                commands[pos] = Command(
                    etCmd.text?.toString()?.trim().orEmpty(),
                    etDesc.text?.toString()?.trim().orEmpty()
                )
                adapter.notifyItemChanged(pos)
                persistState()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun deleteCommand(pos: Int) {
        commands.removeAt(pos)
        adapter.notifyItemRemoved(pos)
        persistState()
    }

    private fun showDeviceActionMenu(pkg: String) {
        Toast.makeText(this, "设备操作将在第二节接入", Toast.LENGTH_SHORT).show()
    }

    private fun persistState() {
        val arr = JSONArray()
        commands.forEach {
            val o = JSONObject()
            o.put("command", it.command)
            o.put("desc", it.desc)
            arr.put(o)
        }
        File(filesDir, "commands.json").writeText(arr.toString())
        File(filesDir, "notes.json").writeText(JSONArray(notes).toString())
    }

    private fun loadState() {
        val cmdFile = File(filesDir, "commands.json")
        if (cmdFile.exists()) {
            val arr = JSONArray(cmdFile.readText())
            commands.clear()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                commands.add(Command(o.getString("command"), o.optString("desc")))
            }
        }
        val noteFile = File(filesDir, "notes.json")
        if (noteFile.exists()) {
            val arr = JSONArray(noteFile.readText())
            notes.clear()
            for (i in 0 until arr.length()) notes.add(arr.getString(i))
        }
    }
}

data class Command(val command: String, val desc: String)

class CommandAdapter(
    private val list: List<Command>,
    private val onRun: (Int) -> Unit,
    private val onEdit: (Int) -> Unit
) : RecyclerView.Adapter<CommandAdapter.VH>() {

    class VH(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        val title: android.widget.TextView = itemView.findViewById(R.id.itemCommand)
        val desc: android.widget.TextView = itemView.findViewById(R.id.itemDesc)
        val btnRun: MaterialButton = itemView.findViewById(R.id.btnItemRun)
        val btnEdit: MaterialButton = itemView.findViewById(R.id.btnItemEdit)
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, type: Int): VH {
        val v = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_command, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, pos: Int) {
        val c = list[pos]
        holder.title.text = c.command
        holder.desc.text = c.desc
        holder.btnRun.setOnClickListener { onRun(pos) }
        holder.btnEdit.setOnClickListener { onEdit(pos) }
    }

    override fun getItemCount() = list.size
}
