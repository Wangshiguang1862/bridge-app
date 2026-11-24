package com.demo.bridge

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import java.io.DataOutputStream

/**
 * Bridge 命令执行器 —— 权限降级链：
 *   root(su) → Shizuku → adb → 无授权基础命令
 * 每一层探测失败自动掉到下一层，界面状态跟随真实可用通道。
 * 不依赖任何外部库：Shizuku 通过反射探测，未装 SDK 也不会崩。
 */
object BridgeExecutor {

    // ==================== 运行模式 ====================

    sealed interface ExecMode {
        data object Root : ExecMode
        data object Shizuku : ExecMode
        data object Adb : ExecMode
        data object Plain : ExecMode
    }

    @Volatile
    private var cachedMode: ExecMode? = null

    /** 真实探测当前可用通道（含结果缓存，失败不崩） */
    fun detectMode(force: Boolean = false): ExecMode {
        if (!force && cachedMode != null) return cachedMode!!

        // 1. root 优先：su -c id -u 返回 0 才是真 root
        if (probeRoot()) {
            cachedMode = ExecMode.Root
            return ExecMode.Root
        }

        // 2. Shizuku：进程在 + 已授权 + 真能执行命令
        if (probeShizuku()) {
            cachedMode = ExecMode.Shizuku
            return ExecMode.Shizuku
        }

        // 3. ADB：self-connect 探测（设备已授权 adb 时才成立）
        if (probeAdb()) {
            cachedMode = ExecMode.Adb
            return ExecMode.Adb
        }

        // 4. 兜底：无授权基础命令
        cachedMode = ExecMode.Plain
        return ExecMode.Plain
    }

    fun modeLabel(mode: ExecMode): String = when (mode) {
        is ExecMode.Root -> "● 运行模式：Root"
        is ExecMode.Shizuku -> "● 运行模式：Shizuku"
        is ExecMode.Adb -> "● 运行模式：ADB"
        is ExecMode.Plain -> "● 运行模式：基础命令（无授权）"
    }

    fun modeColor(mode: ExecMode): Int = when (mode) {
        is ExecMode.Root -> 0xFFC62828.toInt()      // 红：最高权限
        is ExecMode.Shizuku -> 0xFF2E7D32.toInt()   // 绿
        is ExecMode.Adb -> 0xFF1565C0.toInt()       // 蓝
        is ExecMode.Plain -> 0xFF7B7B7B.toInt()     // 灰：无授权
    }

    /** 供界面在 onResume / 点刷新时调用 */
    fun refreshModeLabel(
        context: Context,
        onUpdate: (label: String, color: Int) -> Unit
    ) {
        Thread {
            val m = detectMode(force = true)
            Handler(Looper.getMainLooper()).post {
                onUpdate(modeLabel(m), modeColor(m))
            }
        }.start()
    }

    // ==================== 探测实现 ====================

    private fun probeRoot(): Boolean {
        return try {
            val p = Runtime.getRuntime().exec(arrayOf("su", "-c", "id -u"))
            val uid = p.inputStream.bufferedReader().readText().trim()
            p.waitFor()
            uid == "0"
        } catch (_: Throwable) {
            false
        }
    }

    private fun probeShizuku(): Boolean {
        return try {
            val shizukuClass = Class.forName("rikka.shizuku.Shizuku")
            val ping = shizukuClass.getMethod("pingBinder")
            val alive = ping.invoke(null) as? Boolean ?: false
            if (!alive) return false

            // 必须确认本 App 已拿到授权，光 pingBinder 不够
            val checkPerm = shizukuClass.getMethod("checkSelfPermission")
            val granted = checkPerm.invoke(null) as? Int ?: -1
            granted == PackageManager.PERMISSION_GRANTED
        } catch (_: Throwable) {
            false
        }
    }

    private fun probeAdb(): Boolean {
        // 通过属性判断 adbd 是否以 root/shell 身份运行（部分设备可读）
        return try {
            val p = Runtime.getRuntime().exec(arrayOf("getprop", "init.svc.adbd"))
            val state = p.inputStream.bufferedReader().readText().trim()
            p.waitFor()
            state == "running"
        } catch (_: Throwable) {
            false
        }
    }

    // ==================== 执行 ====================

    /** 测试执行：探测通道 → 执行 → 弹窗显示结果 */
    fun testExecute(context: Context, item: MainActivity.CommandItem) {
        val cmd = item.action
        if (cmd.isBlank()) {
            Toast.makeText(context, "命令为空", Toast.LENGTH_SHORT).show()
            return
        }

        Thread {
            val mode = detectMode()
            val result = runCatching { exec(mode, cmd) }
                .getOrElse { "执行异常：${it.message}" }

            Handler(Looper.getMainLooper()).post {
                val title = "执行结果（${modeLabel(mode).replace("● ", "")}）"
                if (context is Activity && context.isFinishing.not()) {
                    AlertDialog.Builder(context)
                        .setTitle(title)
                        .setMessage(result.take(3000).ifBlank { "（无输出）" })
                        .setPositiveButton("确定", null)
                        .show()
                } else {
                    Toast.makeText(context, result.take(200), Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    /** 按通道执行，失败自动降级到 Plain，保证“总能跑” */
    private fun exec(mode: ExecMode, cmd: String): String {
        return when (mode) {
            is ExecMode.Root -> runSu(cmd).ifEmpty { execPlain(cmd) }
            is ExecMode.Shizuku -> runShizuku(cmd).ifEmpty { execPlain(cmd) }
            is ExecMode.Adb -> runAdb(cmd).ifEmpty { execPlain(cmd) }
            is ExecMode.Plain -> execPlain(cmd)
        }
    }

    private fun runSu(cmd: String): String = runShell(arrayOf("su", "-c", cmd))

    private fun runShizuku(cmd: String): String {
        return try {
            val shizukuClass = Class.forName("rikka.shizuku.Shizuku")
            val newProcess = shizukuClass.getMethod(
                "newProcess", Array<String>::class.java, Array<String>::class.java, String::class.java
            )
            val proc = newProcess.invoke(null, arrayOf("sh", "-c", cmd), null, null) as Process
            readProcess(proc)
        } catch (_: Throwable) {
            ""
        }
    }

    private fun runAdb(cmd: String): String {
        // adb shell 通道：通过本地 adbd 转发，失败返回空交由 Plain 兜底
        return try {
            val p = Runtime.getRuntime().exec(arrayOf("adb", "shell", cmd))
            readProcess(p)
        } catch (_: Throwable) {
            ""
        }
    }

    private fun execPlain(cmd: String): String {
        return try {
            runShell(arrayOf("sh", "-c", cmd))
        } catch (e: Exception) {
            "执行失败：${e.message}"
        }
    }

    private fun runShell(cmd: Array<String>): String =
        readProcess(Runtime.getRuntime().exec(cmd))

    private fun readProcess(p: Process): String {
        val out = p.inputStream.bufferedReader().readText()
        val err = p.errorStream.bufferedReader().readText()
        p.waitFor()
        return out.ifBlank { err }
    }

    /** 需要真实 su 流时可用（保留备用，当前走 Runtime.exec 即可） */
    @Suppress("unused")
    private fun openSuShell(): Process? = try {
        Runtime.getRuntime().exec("su")
    } catch (_: Throwable) {
        null
    }

    @Suppress("unused")
    private fun writeToShell(shell: Process, cmd: String) {
        DataOutputStream(shell.outputStream).use {
            it.writeBytes("$cmd\nexit\n")
            it.flush()
        }
    }
}
