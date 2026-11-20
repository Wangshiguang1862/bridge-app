package com.demo.bridge

import android.app.Application

class BridgeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        DhizukuAuth.init(this)
    }
}
