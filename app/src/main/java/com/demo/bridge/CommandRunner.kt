package com.demo.bridge

import android.content.Context
import android.content.Intent

object CommandRunner {

    fun loadCommands(context: Context): List<CommandItem> =
        BridgeData.loadItems(context)

    fun findByName(context: Context, name: String): CommandItem? =
        loadCommands(context).firstOrNull { it.name == name }

    fun run(context: Context, item: CommandItem) {
        if (!item.enabled) return

        val intent = Intent(item.action).apply {
            if (item.pkg.isNotBlank()) setPackage(item.pkg)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun runByName(context: Context, name: String): Boolean {
        val item = findByName(context, name) ?: return false
        run(context, item)
        return true
    }
}
