package com.demo.bridge

import android.util.Log

object ShizukuDelegate {

    private const val TAG = "BridgeShizuku"
    private val clazz by lazy { Class.forName("rikka.shizuku.Shizuku") }

    fun available(): Boolean {
        return try {
            val code = clazz
                .getMethod("checkPermission", Int::class.javaPrimitiveType)
                .invoke(null, 0) as Int
            Log.d(TAG, "checkPermission=$code")
            code == 0
        } catch (e: Throwable) {
            Log.d(TAG, "available error: ${e.javaClass.simpleName} ${e.message}")
            false
        }
    }

    fun exec(cmd: String): Pair<Int, String> {
        return try {
            val binder = clazz.getMethod("getBinder").invoke(null)
            val stub = Class.forName("rikka.shizuku.ShizukuUserService")
            // 走 newProcess：优先 (String, Array<String>, Array<String>)
            val method = try {
                binder.javaClass.getMethod(
                    "newProcess",
                    String::class.java,
                    Array<String>::class.java,
                    Array<String>::class.java
                )
            } catch (_: NoSuchMethodException) {
                binder.javaClass.getMethod(
                    "newProcess",
                    String::class.java,
                    Array<String>::class.java
                )
            }
            val args = if (method.parameterCount == 3) {
                arrayOf("sh", arrayOf("-c", cmd), emptyArray<String>())
            } else {
                arrayOf("sh", arrayOf("-c", cmd))
            }
            val proc = method.invoke(binder, *args)
            val outField = proc.javaClass.getField("stdout")
            val errField = proc.javaClass.getField("stderr")
            val stdout = outField.get(proc) as String
            val stderr = errField.get(proc) as String
            val waitFor = proc.javaClass.getMethod("waitFor")
            val code = waitFor.invoke(proc) as Int
            code to (stdout + stderr).trim()
        } catch (e: Throwable) {
            Log.d(TAG, "exec error: ${e.javaClass.simpleName} ${e.message}")
            -1 to "错误: ${e.message}"
        }
    }
}
