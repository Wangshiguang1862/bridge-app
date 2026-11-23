package com.demo.bridge

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat

object Notifier {
    private const val CH = "bridge_exec"

    fun init(ctx: Context) {
        val mgr = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (mgr.getNotificationChannel(CH) == null) {
            mgr.createNotificationChannel(
                NotificationChannel(CH, "Bridge执行", NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    fun notify(ctx: Context, cmd: String, ok: Boolean, code: Int) {
        val mgr = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mgr.notify(
            System.currentTimeMillis().toInt(),
            NotificationCompat.Builder(ctx, CH)
                .setSmallIcon(android.R.drawable.ic_menu_info_details)
                .setContentTitle(if (ok) "执行成功" else "执行失败")
                .setContentText("$cmd  返回码:$code")
                .setAutoCancel(true)
                .build()
        )
    }
}
