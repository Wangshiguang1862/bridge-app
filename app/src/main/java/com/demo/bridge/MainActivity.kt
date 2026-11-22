package com.demo.bridge

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import rikka.shizuku.Shizuku

class MainActivity : AppCompatActivity() {

    private val requestCode = 1001
    private val commands = mutableListOf<String>()
    private lateinit var adapter: ArrayAdapter<String>

    private val permissionListener = Shizuku.OnRequestPermissionResultListener { _, grantResult ->
        runOnUiThread {
            if (grantResult == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this@MainActivity, "Shizuku 已授权", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@MainActivity, "Shizuku 未授权", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_command)

        val btnAdd = findViewById<Button>(R.id.btnAdd)
        val btnImport = findViewById<Button>(R.id.btnImport)
        val listView = findViewById<ListView>(R.id.listView)

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, commands)
        listView.adapter = adapter

        btnAdd.setOnClickListener {
            commands.add("命令 ${commands.size + 1}")
            adapter.notifyDataSetChanged()
            Toast.makeText(this, "已添加", Toast.LENGTH_SHORT).show()
        }

        btnImport.setOnClickListener {
            Toast.makeText(this, "导入功能待接入", Toast.LENGTH_SHORT).show()
        }

        Shizuku.addRequestPermissionResultListener(permissionListener)

        if (Shizuku.isPreV11() || Shizuku.getVersion() < 11) {
            Toast.makeText(this, "Shizuku 版本过低", Toast.LENGTH_SHORT).show()
        } else if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            Shizuku.requestPermission(requestCode)
        } else {
            Toast.makeText(this, "Shizuku 已授权", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Shizuku.removeRequestPermissionResultListener(permissionListener)
    }
}
