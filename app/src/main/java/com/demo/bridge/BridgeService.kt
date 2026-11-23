package com.demo.bridge
import android.app.Service
import android.content.Intent
import android.os.IBinder
class BridgeService : Service() {
    override fun onBind(i: Intent?): IBinder? = null
    override fun onStartCommand(i: Intent?, f: Int, s: Int): Int = START_STICKY
}
