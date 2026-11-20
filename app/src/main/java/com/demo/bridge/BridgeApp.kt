package com.demo.bridge

import android.app.Application
import android.util.Log
import com.rosan.dhizuku.api.Dhizuku

class BridgeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.d("Bridge", "app onCreate")
        try {
            if (Dhizuku.isPermissionGranted()) {
                Dhizuku.init(this)
                Log.d("Bridge", "dhizuku init ok")
            } else {
                Log.d("Bridge", "dhizuku not granted")
            }
        } catch (e: Throwable) {
            Log.e("Bridge", "dhizuku init fail", e)
        }
    }
}
