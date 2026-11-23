package com.demo.bridge

import android.content.Context
import rikka.shizuku.Shizuku
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CommandRunner {

    private const val DEFAULT_TIMEOUT_MS = 10_000L

    fun run(context: Context, item: MainActivity.CommandItem, useShizuku: Boolean): ExecLog {
        val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        var retCode = -1
        var stdout = ""
        var stderr = ""

        try {
            if (useShizuku) {
                if (!Shizuku.pingBinder()) {
                    stderr = "Shizuku 未就绪"
                } else {
                    val cmd = buildCommand(item)
                    val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", cmd))
                    stdout = process.inputStream.bufferedReader().use { it.readText() }
                    stderr = process.errorStream.bufferedReader().use { it.readText() }
                    retCode = process.waitFor()
                }
            } else {
                val cmd = buildCommand(item)
                val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", cmd))
                stdout = process.inputStream.bufferedReader().use { it.readText() }
                stderr = process.errorStream.bufferedReader().use { it.readText() }
                retCode = process.waitFor()
            }
        } catch (e: Exception) {
            stderr = e.message ?: "未知异常"
            retCode = -1
        }

        val log = ExecLog(
            time = time,
            cmdName = item.name,
            mode = if (useShizuku) "shizuku" else "normal",
            retCode = retCode,
            stdout = stdout,
            stderr = stderr
        )
        LogStore.add(log)
        Notifier.show(context, item.name, retCode)
        return log
    }

    private fun buildCommand(item: MainActivity.CommandItem): String {
        return when {
            item.action.startsWith("am ") ||
            item.action.startsWith("pm ") ||
            item.action.startsWith("cmd ") -> item.action
            else -> "am start -n ${item.pkg}/${item.action}"
        }
    }
}
