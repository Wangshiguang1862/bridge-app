package com.demo.bridge

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log

class BridgeService : Service() {

    companion object {
        private const val TAG = "Bridge"
        private const val CHANNEL_ID = "bridge_channel"
        private const val NOTIFICATION_ID = 1001
    }

    private val binder = object : IBridgeApi.Stub() {
        override fun execute(command: String?): String {
            Log.d(TAG, "execute: $command")
            if (!DhizukuAuth.isReady()) {
                return "ERR: dhizuku not ready"
            }
            return try {
                doExecute(command.orEmpty())
            } catch (t: Throwable) {
                Log.w(TAG, "execute failed", t)
                "ERR: ${t.message ?: t.javaClass.simpleName}"
            }
        }

        override fun isReady(): Boolean = DhizukuAuth.isReady()
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "service onCreate")
        createChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "service onStartCommand")
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        Log.d(TAG, "service onDestroy")
        super.onDestroy()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Bridge",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Bridge")
                .setContentText("Bridge service running")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("Bridge")
                .setContentText("Bridge service running")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .build()
        }
    }

    /**
     * 真实 DeviceOwner 业务入口，待确认具体能力后替换。
     * 可接入：静默安装/卸载、shell 命令、系统设置修改、应用隐藏等。
     */
    private fun doExecute(command: String): String {
        return DhizukuAuth.run {
            // TODO: 按 Binder 契约实现真实调用
            "OK: received=$command"
        }
    }
}
