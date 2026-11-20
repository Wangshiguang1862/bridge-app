package com.demo.bridge

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat

class BridgeService : Service() {

    companion object {
        const val CHANNEL_ID = "bridge"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("Bridge", "service onCreate")
        createChannel()
        startForeground(1, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val cmd = intent?.getStringExtra("cmd")
        Log.d("Bridge", "service cmd=$cmd")
        if (!cmd.isNullOrBlank()) execute(cmd)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_ID,
                        "Bridge",
                        NotificationManager.IMPORTANCE_LOW
                    )
                )
            }
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Bridge running")
            .setOngoing(true)
            .build()
    }

    private fun execute(cmd: String) {
        try {
            val p = Runtime.getRuntime().exec(arrayOf("/system/bin/sh", "-c", cmd))
            val out = p.inputStream.bufferedReader().readText()
            val err = p.errorStream.bufferedReader().readText()
            p.waitFor()
            Log.d("Bridge", "exit=${p.exitValue()} out=$out err=$err")
        } catch (e: Exception) {
            Log.e("Bridge", "exec error", e)
        }
    }
}
