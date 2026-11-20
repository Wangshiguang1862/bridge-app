package com.demo.bridge

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.rosan.dhizuku.api.Dhizuku
import com.rosan.dhizuku.api.DhizukuRequestPermissionListener

class BridgeService : Service() {
    companion object {
        private const val CHANNEL_ID = "bridge_channel"
        private const val NOTIFY_ID = 1
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(NOTIFY_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val cmd = intent?.getStringExtra("cmd")
        if (!cmd.isNullOrBlank()) {
            execute(cmd)
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Bridge Service",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Bridge Running")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("Bridge Running")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .build()
        }
    }

    private fun execute(cmd: String) {
        Log.d("Bridge", "execute cmd: $cmd")
        if (!Dhizuku.isPermissionGranted()) {
            Log.w("Bridge", "dhizuku not granted, skip")
            return
        }
        try {
            // 示例：通过 Dhizuku 执行 shell（按实际业务替换）
            val result = Dhizuku.getRemoteBinder()
            Log.d("Bridge", "remote binder: $result")
            // TODO: 把 cmd 映射为具体 Dhizuku DeviceOwner API 调用
        } catch (e: Throwable) {
            Log.e("Bridge", "execute fail", e)
        }
    }
}
