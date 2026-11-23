package com.demo.bridge

object LogStore {
    private const val MAX = 20
    private val logs = ArrayDeque<ExecLog>()

    @Synchronized
    fun add(log: ExecLog) {
        logs.addLast(log)
        while (logs.size > MAX) logs.removeFirst()
    }

    @Synchronized
    fun all(): List<ExecLog> = logs.toList()

    @Synchronized
    fun clear() = logs.clear()
}

data class ExecLog(
    val time: String,
    val cmdName: String,
    val mode: String,
    val retCode: Int,
    val stdout: String,
    val stderr: String
)
