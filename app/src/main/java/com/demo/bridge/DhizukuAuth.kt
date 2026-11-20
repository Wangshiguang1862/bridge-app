package com.demo.bridge

import android.content.Context
import android.content.pm.PackageManager
import com.rosan.dhizuku.api.Dhizuku
import com.rosan.dhizuku.api.DhizukuRequestPermissionListener

object DhizukuAuth {

    fun isReady(): Boolean = try {
        Dhizuku.isPermissionGranted() && Dhizuku.getRemoteBinder() != null
    } catch (e: Throwable) {
        false
    }

    fun ensurePermission(
        context: Context,
        onGranted: () -> Unit,
        onDenied: () -> Unit
    ) {
        try {
            if (isReady()) {
                onGranted()
                return
            }
            Dhizuku.requestPermission(object : DhizukuRequestPermissionListener() {
                override fun onRequestPermissionResult(grantResult: Int) {
                    if (grantResult == PackageManager.PERMISSION_GRANTED) {
                        onGranted()
                    } else {
                        onDenied()
                    }
                }
            })
        } catch (e: Throwable) {
            onDenied()
        }
    }
}
