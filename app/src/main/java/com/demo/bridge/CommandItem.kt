package com.demo.bridge

data class CommandItem(
    val name: String,
    val pkg: String = "",
    val action: String,
    val enabled: Boolean = false
)
