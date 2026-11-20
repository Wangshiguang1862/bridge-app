package com.demo.bridge

import android.app.Application
import android.util.Log

class BridgeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.d("Bridge", "app onCreate")
        DhizukuAuth.init(this)
    }
}
