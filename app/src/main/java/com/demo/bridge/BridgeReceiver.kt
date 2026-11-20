package com.demo.bridge

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class BridgeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("Bridge", "onReceive hit action=${intent.action}")
        val cmd = intent.getStringExtra("cmd") ?: return
        Log.d("Bridge", "recv cmd=$cmd")
        val i = Intent(context, BridgeService::class.java).apply {
            putExtra("cmd", cmd)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(i)
        } else {
            context.startService(i)
        }
    }
}
