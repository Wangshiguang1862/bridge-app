package com.demo.bridge

import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import java.io.DataOutputStream
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.functions
import kotlin.reflect.jvm.isAccessible

/**
 * Bridge 命令执行器 — 权限降级链：
 *   root(su) → Shizuku → adb → 无授权基础命令
 * 每一层探测失败自动掉到下一层，界面状态跟随真实可用通道。
 * 不依赖任何外部库：Shizuku 通过反射探测，未装 SDK 也不会崩。
 */
object BridgeExecutor {

    sealed interface ExecMode {
        data object Root : ExecMode
        data object Shizuku : ExecMode
        data object Adb : ExecMode
        data object Plain : ExecMode
    }

    @Volatile
    private var cachedMode: ExecMode? = null

    // ============ 对外接口 ============

    fun detectMode(force: Boolean = false): ExecMode {
        if (!force) cachedMode?.let { return it }
        val mode = probeMode()
        cachedMode = mode
        return mode
    }

    fun refreshModeLabel(
        context: Context,
        callback: (label: String, color: Int) -> Unit
    ) {
        val mode = detectMode(force = true)
        val (label, color) = when (mode) {
            ExecMode.Root -> "● 运行模式：Root" to Color.parseColor("#FF5252")
            ExecMode.Shizuku -> "● 运行模式：Shizuku" to Color.parseColor("#FF9800")
            ExecMode.Adb -> "● 运行模式：ADB" to Color.parseColor("#2196F3")
            ExecMode.Plain -> "● 运行模式：基础命令（无授权）" to Color.parseColor("#4CAF50")
        }
        callback(label, color)
    }

    fun testExecute(context: Context, item: MainActivity.CommandItem) {
        val cmd = item.action.trim()
        if (cmd.isEmpty()) {
            Toast.makeText(context, "命令为空", Toast.LENGTH_SHORT).show()
            return
        }

        Thread {
            val mode = detectMode()
            val result = executeWithMode(mode, cmd)
            Handler(Looper.getMainLooper()).post {
                AlertDialog.Builder(context)
                    .setTitle("执行结果（${modeName(mode)}）")
                    .setMessage(result.take(4000))
                    .setPositiveButton("确定", null)
                    .show()
            }
        }.start()
    }

    fun modeName(mode: ExecMode): String = when (mode) {
        ExecMode.Root -> "Root"
        ExecMode.Shizuku -> "Shizuku"
        ExecMode.Adb -> "ADB"
        ExecMode.Plain -> "基础命令"
    }

    // ============ 探测链 ============

    private fun probeMode(): ExecMode {
        if (probeRoot()) return ExecMode.Root
        if (probeShizuku()) return ExecMode.Shizuku
        if (probeAdb()) return ExecMode.Adb
        return ExecMode.Plain
    }

    private fun probeRoot(): Boolean = try {
        val p = Runtime.getRuntime().exec(arrayOf("su", "-c", "id -u"))
        val uid = p.inputStream.bufferedReader().use { it.readText() }.trim()
        p.destroy()
        uid == "0"
    } catch (_: Throwable) {
        false
    }

    private fun probeShizuku(): Boolean = try {
        val cls = Class.forName("rikka.shizuku.Shizuku")
        val ping = cls.getMethod("pingBinder")
        if (ping.invoke(null) != true) return false
        val checkPerm = cls.getMethod("checkSelfPermission")
        val perm = checkPerm.invoke(null) as Int
        if (perm != PackageManager.PERMISSION_GRANTED) return false
        // 再跑一次真实命令确认通道可用
        val newProcess = cls.getMethod(
            "newProcess",
            Array<String>::class.java,
            Array<String>::class.java,
            String::class.java
        )
        val proc = newProcess.invoke(null, arrayOf("sh", "-c", "id -u"), null, null) as Process
        val out = proc.inputStream.bufferedReader().use { it.readText() }.trim()
        proc.destroy()
        out.isNotBlank()
    } catch (_: Throwable) {
        false
    }

    private fun probeAdb(): Boolean = try {
        // 占位：后续可自行起 adbd 并通过 local socket 探测
        false
    } catch (_: Throwable) {
        false
    }

    // ============ 执行链 ============

    private fun executeWithMode(mode: ExecMode, cmd: String): String {
        return when (mode) {
            ExecMode.Root -> runCatching { execSu(cmd) }.getOrElse { execPlain(cmd) }
            ExecMode.Shizuku -> runCatching { execShizuku(cmd) }.getOrElse { execPlain(cmd) }
            ExecMode.Adb -> runCatching { execAdb(cmd) }.getOrElse { execPlain(cmd) }
            ExecMode.Plain -> execPlain(cmd)
        }
    }

    private fun execSu(cmd: String): String {
        val p = Runtime.getRuntime().exec(arrayOf("su", "-c", cmd))
        return readProcess(p)
    }

    private fun execShizuku(cmd: String): String {
        val cls = Class.forName("rikka.shizuku.Shizuku")
        val newProcess = cls.getMethod(
            "newProcess",
            Array<String>::class.java,
            Array<String>::class.java,
            String::class.java
        )
        val proc = newProcess.invoke(null, arrayOf("sh", "-c", cmd), null, null) as Process
        return readProcess(proc)
    }

    private fun execAdb(cmd: String): String {
        // 预留：后续接入 adbd local socket
        return execPlain(cmd)
    }

    private fun execPlain(cmd: String): String {
        val p = Runtime.getRuntime().exec(arrayOf("sh", "-c", cmd))
        return readProcess(p)
    }

    private fun readProcess(p: Process): String {
        val out = p.inputStream.bufferedReader().use { it.readText() }
        val err = p.errorStream.bufferedReader().use { it.readText() }
        p.waitFor()
        p.destroy()
        return if (out.isNotBlank()) out else if (err.isNotBlank()) err else "(无输出)"
    }
}
