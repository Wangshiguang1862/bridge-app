package com.demo.bridge

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import java.io.File

class BridgeService : Service() {

    companion object {
        private const val TAG = "Bridge"
        private const val CHANNEL_ID = "bridge_result"
        private const val NOTIFY_ID_BASE = 2000
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "service onCreate")
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand")
        val indices = intent?.getIntegerArrayListExtra("indices")
        if (!indices.isNullOrEmpty()) {
            executeByIndices(indices)
        }
        stopSelf(startId)
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Bridge 执行结果",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun executeByIndices(indices: List<Int>) {
        val dir = File(filesDir, "commands")
        if (!dir.exists() || !dir.isDirectory) {
            notifyResult("Bridge", "命令目录不存在")
            return
        }

        val files = dir.listFiles { f -> f.isFile && f.name.endsWith(".sh") }
            ?.sortedBy { it.name } ?: emptyArray()

        if (files.isEmpty()) {
            notifyResult("Bridge", "无可用命令")
            return
        }

        val results = mutableListOf<String>()
        indices.forEach { idx ->
            if (idx < 1 || idx > files.size) {
                results.add("序号$idx: 不存在")
                return@forEach
            }
            val file = files[idx - 1]
            val command = file.readText().trim()
            if (command.isEmpty()) {
                results.add("${file.nameWithoutExtension}: 空命令")
                return@forEach
            }
            val result = doExecute(command)
            val name = file.nameWithoutExtension
            if (result.contains("\"ok\":true")) {
                results.add("$name: 成功")
            } else {
                val reason = result.substringAfter("\"err\":\"", "失败").trimEnd('"')
                results.add("$name: 失败($reason)")
            }
        }

        notifyResult("Bridge 执行完成", results.joinToString(" | "))
    }

    private fun doExecute(command: String): String {
        return try {
            if (!DhizukuAuth.isReady()) return err("dhizuku not ready")
            val parts = command.split(":", limit = 2)
            val action = parts[0].lowercase()
            val arg = parts.getOrNull(1)?.trim().orEmpty()
            when (action) {
                "install" -> ok("reserved:install:$arg")
                "uninstall" -> ok("reserved:uninstall:$arg")
                "shell" -> ok("reserved:shell:$arg")
                "setting" -> ok("reserved:setting:$arg")
                "app" -> ok("reserved:app:$arg")
                "ping" -> ok("pong")
                "status" -> ok(if (DhizukuAuth.isReady()) "ready" else "not-ready")
                else -> err("unsupported: $action")
            }
        } catch (t: Throwable) {
            err(t.message ?: "unknown")
        }
    }

    private fun notifyResult(title: String, content: String) {
        val nm = getSystemService(NotificationManager::class.java)
        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setAutoCancel(true)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setAutoCancel(true)
                .build()
        }
        nm.notify(NOTIFY_ID_BASE + System.currentTimeMillis().toInt(), notification)
    }

    private fun ok(data: String) = """{"ok":true,"data":"$data"}"""
    private fun err(msg: String) = """{"ok":false,"err":"$msg"}"""
}
