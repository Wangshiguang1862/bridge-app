package com.demo.bridge

import android.content.Context

object BridgeExecutor {

    private var cachedMode: String = "plain"

    fun invalidate() {
        cachedMode = "plain"
    }

    fun modeLabel(): String = cachedMode

    fun refreshModeLabel(context: Context, callback: (String) -> Unit) {
        Thread {
            val mode = if (ShizukuDelegate.available()) {
                "shizuku"
            } else {
                val result = exec("id")
                if (result.contains("uid=0")) "root" else "plain"
            }
            cachedMode = mode
            callback.invoke(mode)
        }.start()
    }

    fun execByType(type: String, command: String): String {
        return when (type) {
            "delay" -> {
                val ms = command.toLongOrNull() ?: 0L
                if (ms > 0) Thread.sleep(ms)
                "delayed ${ms}ms"
            }
            "js" -> {
                if (!command.endsWith(".js")) {
                    return "js 类型 action 必须是 .js 文件路径"
                }
                val scriptFile = java.io.File(command)
                if (!scriptFile.exists()) {
                    return "脚本文件不存在: $command"
                }
                val runtime = java.lang.Runtime.getRuntime()
                return try {
                    val process = runtime.exec(arrayOf("js", command))
                    process.waitFor()
                    val out = process.inputStream.bufferedReader().readText()
                    val err = process.errorStream.bufferedReader().readText()
                    if (out.isNotBlank()) out else if (err.isNotBlank()) err else "(无输出)"
                } catch (_: Throwable) {
                    if (ShizukuDelegate.available()) {
                        try {
                            val process = runtime.exec(arrayOf("node", command))
                            process.waitFor()
                            val out = process.inputStream.bufferedReader().readText()
                            val err = process.errorStream.bufferedReader().readText()
                            if (out.isNotBlank()) out else if (err.isNotBlank()) err else "(无输出)"
                        } catch (e: Throwable) {
                            "JS 执行失败: ${e.message}"
                        }
                    } else {
                        "JS 执行需要系统 js 命令或 Shizuku 环境"
                    }
                }
            }
            else -> {
                exec(command)
            }
        }
    }

    fun exec(command: String): String {
        return try {
            if (ShizukuDelegate.available()) {
                ShizukuDelegate.exec(command)
            } else {
                val process = java.lang.Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
                process.waitFor()
                val out = process.inputStream.bufferedReader().readText()
                val err = process.errorStream.bufferedReader().readText()
                if (out.isNotBlank()) out else if (err.isNotBlank()) err else "(无输出)"
            }
        } catch (e: Throwable) {
            e.message ?: "exec failed"
        }
    }
}
