package com.demo.bridge

import java.io.BufferedReader

object ShizukuDelegate {

    fun available(): Boolean {
        return try {
            val c = Class.forName("rikka.shizuku.Shizuku")
            val m = c.getMethod("checkPermission", Int::class.javaPrimitiveType)
            val code = m.invoke(null, 0) as Int
            code == 0
        } catch (_: Throwable) {
            false
        }
    }

    fun exec(command: String): String {
        return try {
            val shizuku = Class.forName("rikka.shizuku.Shizuku")
            val newProcess = shizuku.getMethod(
                "newProcess",
                String::class.java,
                Array<String>::class.java,
                Array<String>::class.java
            )
            val proc = newProcess.invoke(null, "/system/bin/sh", arrayOf("-c", command), null)
            val waitFor = proc.javaClass.getMethod("waitFor")
            waitFor.invoke(proc)
            val input = proc.javaClass.getMethod("getInputStream").invoke(proc) as java.io.InputStream
            val err = proc.javaClass.getMethod("getErrorStream").invoke(proc) as java.io.InputStream
            val out = input.bufferedReader().readText()
            val errout = err.bufferedReader().readText()
            if (out.isNotBlank()) out else if (errout.isNotBlank()) errout else "(无输出)"
        } catch (e: Throwable) {
            e.message ?: "exec failed"
        }
    }
}
