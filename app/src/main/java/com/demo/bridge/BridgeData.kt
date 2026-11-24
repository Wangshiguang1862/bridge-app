package com.demo.bridge

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * 命令数据读写：统一走 SharedPreferences("bridge_prefs") 的 "commands" 字段，
 * 内容为 JSONArray 字符串。CommandItem 定义在同包 CommandItem.kt 中。
 */
object BridgeData {

    const val PREFS_NAME = "bridge_prefs"
    const val KEY_COMMANDS = "commands"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** 读取全部命令，空数据返回空列表 */
    fun loadItems(context: Context): List<CommandItem> {
        val raw = prefs(context).getString(KEY_COMMANDS, "") ?: ""
        if (raw.isBlank()) return emptyList()

        val result = mutableListOf<CommandItem>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val name = o.optString("name", "").trim()
                if (name.isEmpty()) continue
                result.add(
                    CommandItem(
                        name = name,
                        pkg = o.optString("pkg", "").trim(),
                        action = o.optString("action", "").trim(),
                        enabled = o.optBoolean("enabled", false)
                    )
                )
            }
        } catch (_: Throwable) {
            // 数据损坏时不崩，返回已解析到的部分
        }
        return result
    }

    /** 整体覆盖保存（导入 = 清空替换，也走这里） */
    fun saveItems(context: Context, items: List<CommandItem>) {
        val arr = JSONArray()
        items.forEach { item ->
            val o = JSONObject()
            o.put("name", item.name)
            o.put("pkg", item.pkg)
            o.put("action", item.action)
            o.put("enabled", item.enabled)
            arr.put(o)
        }
        prefs(context).edit().putString(KEY_COMMANDS, arr.toString()).apply()
    }

    /** 追加单条 */
    fun addItem(context: Context, item: CommandItem) {
        val list = loadItems(context).toMutableList()
        list.add(item)
        saveItems(context, list)
    }

    /** 按名称删除 */
    fun removeItem(context: Context, name: String) {
        val list = loadItems(context).filterNot { it.name == name }
        saveItems(context, list)
    }

    /** 序列化为导出用 JSON 数组字符串 */
    fun toJson(items: List<CommandItem>): String {
        val arr = JSONArray()
        items.forEach { item ->
            val o = JSONObject()
            o.put("name", item.name)
            o.put("pkg", item.pkg)
            o.put("action", item.action)
            o.put("enabled", item.enabled)
            arr.put(o)
        }
        return arr.toString()
    }

    /** 解析导入文本，失败返回 null（调用方据此提示，且不改动原数据） */
    fun parseItems(json: String): List<CommandItem>? {
        return try {
            val cleaned = json.trim().removePrefix("\uFEFF")
            val arr = JSONArray(cleaned)
            val list = mutableListOf<CommandItem>()
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val name = o.optString("name", "").trim()
                if (name.isEmpty()) continue
                list.add(
                    CommandItem(
                        name = name,
                        pkg = o.optString("pkg", "").trim(),
                        action = o.optString("action", "").trim(),
                        enabled = o.optBoolean("enabled", false)
                    )
                )
            }
            list
        } catch (_: Throwable) {
            null
        }
    }
}
