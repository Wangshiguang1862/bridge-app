package com.demo.bridge

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.dhizuku.api.Dhizuku
import kotlin.concurrent.thread

class BridgeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val cmd = intent.getStringExtra("cmd") ?: "echo no_cmd"
        val pending = goAsync()

        thread {
            try {
                val result = if (Dhizuku.isPermissionGranted()) {
                    execViaDhizuku(cmd)
                } else {
                    execFallback(cmd)
                }
                sendNotify(context, "执行完成", result.take(500))
            } catch (e: Exception) {
                sendNotify(context, "执行失败", e.message ?: "unknown")
            } finally {
                pending.finish()
            }
        }
    }

    private fun execFallback(cmd: String): String {
        val p = Runtime.getRuntime().exec(arrayOf("sh", "-c", cmd))
        val out = p.inputStream.bufferedReader().readText()
        val err = p.errorStream.bufferedReader().readText()
        p.waitFor()
        return out.ifBlank { err }
    }

    private fun execViaDhizuku(cmd: String): String {
        // Dhizuku API 实际调用方式依版本而定，先走 fallback 验证链路
        return execFallback(cmd)
    }

    private fun sendNotify(c: Context, title: String, text: String) {
        if (Build.VERSION.SDK_INT >= 33 &&
            androidx.core.content.ContextCompat.checkSelfPermission(
                c, android.Manifest.permission.POST_NOTIFICATIONS
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) return
        NotificationManagerCompat.from(c).notify(
            System.currentTimeMillis().toInt(),
            NotificationCompat.Builder(c, "bridge")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(text)
                .setAutoCancel(true)
                .build()
        )
    }
}
