package com.demo.bridge

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.NotificationManager
import androidx.core.app.NotificationCompat

class BridgeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "com.demo.bridge.RUN_COMMAND") return
        val cmd = intent.getStringExtra("cmd") ?: "empty"
        val notification = NotificationCompat.Builder(context, "bridge")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Bridge Command")
            .setContentText(cmd)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        context.getSystemService(NotificationManager::class.java)
            .notify(1, notification)
    }
}
