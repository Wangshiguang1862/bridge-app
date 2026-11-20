package com.demo.bridge

import android.content.Context
import com.rosan.dhizuku.api.Dhizuku

object DhizukuAuth {

    fun init(context: Context) {
        try {
            Dhizuku.init(context)
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    fun isReady(): Boolean {
        return try {
            Dhizuku.isReady()
        } catch (t: Throwable) {
            false
        }
    }
}
