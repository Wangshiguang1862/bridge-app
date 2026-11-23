package com.demo.bridge

import android.content.Context
import java.text.SimpleDateFormat
import java.util.*

object CommandRunner {

    fun run(ctx: Context, item: MainActivity.CommandItem, debug: Boolean): ExecLog {
        val mode = GlobalState.mode
        val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val sbOut = StringBuilder()
        val sbErr = StringBuilder()
        var ret = -1

        try {
            when (mode) {
                RunMode.SHIZUKU -> {
                    if (rikka.shizuku.Shizuku.pingBinder() &&
                        rikka.shizuku.Shizuku.checkSelfPermission() == 0
                    ) {
                        val proc = Runtime.getRuntime().exec(arrayOf("sh", "-c", item.action))
                        sbOut.append(proc.inputStream.bufferedReader().readText())
                        sbErr.append(proc.errorStream.bufferedReader().readText())
                        ret = proc.waitFor()
                    } else {
                        sbErr.append("Shizuku 未授权")
                        ret = -2
                    }
                }
                RunMode.ADB -> {
                    val proc = Runtime.getRuntime().exec(arrayOf("sh", "-c", item.action))
                    sbOut.append(proc.inputStream.bufferedReader().readText())
                    sbErr.append(proc.errorStream.bufferedReader().readText())
                    ret = proc.waitFor()
                }
                RunMode.BASIC -> {
                    sbErr.append("基础模式无执行权限")
                    ret = -3
                }
            }
        } catch (e: Exception) {
            sbErr.append(e.message)
            ret = -4
        }

        val log = ExecLog(
            System.currentTimeMillis().toString(),
            time,
            item.name,
            mode.name,
            ret,
            sbOut.toString(),
            sbErr.toString()
        )
        LogStore.append(ctx, log)

        if (!debug) {
            Notifier.notify(ctx, item.name, ret == 0, ret)
        }

        return log
    }
}
