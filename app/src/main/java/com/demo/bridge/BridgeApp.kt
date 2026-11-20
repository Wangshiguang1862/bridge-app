package com.demo.bridge

import android.app.Application
import android.util.Log
import com.rosan.dhizuku.api.Dhizuku

class BridgeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.d("Bridge", "app onCreate")
        try {
            Dhizuku.init(this)
            Log.d("Bridge", "dhizuku init done")
        } catch (e: Throwable) {
            Log.w("Bridge", "dhizuku init failed, ignore", e)
        }
    }
}
