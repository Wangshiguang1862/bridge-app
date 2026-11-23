package com.demo.bridge

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

data class ExecLog(
    val id: String,
    val time: String,
    val cmdName: String,
    val mode: String,
    val retCode: Int,
    val stdout: String,
    val stderr: String
)

object LogStore {
    private const val MAX = 20

    fun append(ctx: Context, log: ExecLog) {
        val dir = File(ctx.filesDir, "logs")
        if (!dir.exists()) dir.mkdirs()
        val files = dir.listFiles { f -> f.name.endsWith(".txt") }
            ?.sortedBy { it.name } ?: emptyList()
        if (files.size >= MAX) files.first().delete()
        val f = File(dir, "log_${log.id}.txt")
        f.writeText(
            JSONObject().apply {
                put("time", log.time)
                put("cmd", log.cmdName)
                put("mode", log.mode)
                put("ret", log.retCode)
                put("out", log.stdout)
                put("err", log.stderr)
            }.toString()
        )
    }

    fun list(ctx: Context): List<ExecLog> {
        val dir = File(ctx.filesDir, "logs")
        if (!dir.exists()) return emptyList()
        return dir.listFiles { f -> f.name.endsWith(".txt") }
            ?.sortedByDescending { it.name }
            ?.mapNotNull {
                try {
                    val j = JSONObject(it.readText())
                    ExecLog(
                        it.nameWithoutExtension.removePrefix("log_"),
                        j.getString("time"),
                        j.getString("cmd"),
                        j.getString("mode"),
                        j.getInt("ret"),
                        j.getString("out"),
                        j.getString("err")
                    )
                } catch (e: Exception) { null }
            } ?: emptyList()
    }

    fun get(ctx: Context, id: String): ExecLog? =
        list(ctx).find { it.id == id }
}
