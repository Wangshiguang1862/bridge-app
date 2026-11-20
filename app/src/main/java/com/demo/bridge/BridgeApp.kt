package com.demo.bridge

import android.app.Application
import android.util.Log
import com.rosan.dhizuku.api.Dhizuku
import com.rosan.dhizuku.api.DhizukuRequestPermissionListener

class BridgeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.d("Bridge", "app onCreate")
        if (Dhizuku.isPermissionGranted()) {
            Dhizuku.init(this)
            Log.d("Bridge", "dhizuku init ok")
            // 实际命令下发由外部 Dhizuku 客户端调用本 App 暴露的通道；
            // 收到命令后统一转 BridgeService.execute(cmd)
        } else {
            Log.d("Bridge", "dhizuku not granted")
        }
    }
}
