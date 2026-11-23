package com.demo.bridge

enum class RunMode { SHIZUKU, ADB, BASIC }

object GlobalState {
    @Volatile var mode: RunMode = RunMode.BASIC
}
