package com.demo.bridge

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val cmdParam = intent.getStringExtra("cmd")
        if (!cmdParam.isNullOrBlank()) {
            val indices = cmdParam.split(",").mapNotNull { it.trim().toIntOrNull() }
            if (indices.isNotEmpty()) {
                Intent(this, BridgeService::class.java).also {
                    it.putIntegerArrayListExtra("indices", ArrayList(indices))
                    startService(it)
                }
                finish()
                return
            }
        }

        val selected = loadSelected()
        if (selected.isNotEmpty()) {
            Intent(this, BridgeService::class.java).also {
                it.putIntegerArrayListExtra("indices", ArrayList(selected))
                startService(it)
            }
        }

        startActivity(Intent(this, CommandActivity::class.java))
        finish()
    }

    private fun loadSelected(): List<Int> {
        val file = File(filesDir, "selected.json")
        if (!file.exists()) return emptyList()
        return try {
            val text = file.readText().trim()
            if (text.startsWith("[")) {
                text.removeSurrounding("[", "]").split(",")
                    .mapNotNull { it.trim().toIntOrNull() }
            } else emptyList()
        } catch (e: Throwable) {
            emptyList()
        }
    }
}
