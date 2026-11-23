package com.demo.bridge

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BridgeCommandReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // 预留外部触发入口，本版本不实际执行
    }
}
