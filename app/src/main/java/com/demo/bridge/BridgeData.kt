package com.demo.bridge

import android.content.Context
import org.json.JSONArray

object BridgeData {
    fun loadItems(context: Context): List<MainActivity.CommandItem> {
        val prefs = context.getSharedPreferences("bridge_prefs", Context.MODE_PRIVATE)
        val raw = prefs.getString("commands", "") ?: ""
        val list = mutableListOf<MainActivity.CommandItem>()
        if (raw.isBlank()) return list
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(MainActivity.CommandItem(
                    o.optString("name"),
                    o.optString("pkg"),
                    o.optString("action"),
                    o.optBoolean("enabled", false)
                ))
            }
        } catch (_: Exception) {}
        return list
    }
}
