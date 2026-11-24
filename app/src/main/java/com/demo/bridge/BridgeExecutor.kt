package com.demo.bridge

import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import java.io.DataOutputStream

/**
 * Bridge 命令执行器 — 权限降级链：
 * root(su) → Shizuku → adb → 无授权基础命令
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

    fun invalidate() {
        cachedMode = null
    }

    fun currentMode(): ExecMode {
        cachedMode?.let { return it }
        val mode = detectMode()
        cachedMode = mode
        return mode
    }

    fun modeLabel(): String = when (currentMode()) {
        ExecMode.Root -> "● 运行模式：Root"
        ExecMode.Shizuku -> "● 运行模式：Shizuku"
        ExecMode.Adb -> "● 运行模式：ADB"
        ExecMode.Plain -> "● 运行模式：基础命令（无授权）"
    }

    fun refreshModeLabel(
        context: Context,
        apply: (String) -> Unit
    ) {
        val label = modeLabel()
        Handler(Looper.getMainLooper()).post {
            apply(label)
        }
    }

    fun exec(action: String): String {
        return when (currentMode()) {
            ExecMode.Root -> runCatching { execRoot(action) }
                .getOrElse { execPlain(action) }
            ExecMode.Shizuku -> runCatching { execShizuku(action) }
                .getOrElse { execPlain(action) }
            ExecMode.Adb -> execPlain(action)
            ExecMode.Plain -> execPlain(action)
        }
    }

    private fun detectMode(): ExecMode {
        if (probeRoot()) return ExecMode.Root
        if (probeShizuku()) return ExecMode.Shizuku
        return ExecMode.Plain
    }

    private fun probeRoot(): Boolean {
        return try {
            val p = Runtime.getRuntime().exec(arrayOf("su", "-c", "id -u"))
            val uid = p.inputStream.bufferedReader().use { it.readText() }.trim()
            p.waitFor()
            p.destroy()
            uid == "0"
        } catch (_: Throwable) {
            false
        }
    }

    private fun probeShizuku(): Boolean {
        return try {
            val cls = Class.forName("rikka.shizuku.Shizuku")
            val ping = cls.getMethod("pingBinder")
            if (ping.invoke(null) != true) return false

            val perm = cls.getMethod("checkSelfPermission").invoke(null) as Int
            if (perm != PackageManager.PERMISSION_GRANTED) return false

            val newProcess = cls.getMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            val proc = newProcess.invoke(
                null,
                arrayOf("sh", "-c", "id -u"),
                null,
                null
            ) as Process
            val out = proc.inputStream.bufferedReader().use { it.readText() }.trim()
            proc.waitFor()
            proc.destroy()
            out.isNotBlank()
        } catch (_: Throwable) {
            false
        }
    }

    private fun execRoot(cmd: String): String {
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
        val proc = newProcess.invoke(
            null,
            arrayOf("sh", "-c", cmd),
            null,
            null
        ) as Process
        return readProcess(proc)
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
        return when {
            out.isNotBlank() -> out
            err.isNotBlank() -> err
            else -> "(无输出)"
        }
    }
}
