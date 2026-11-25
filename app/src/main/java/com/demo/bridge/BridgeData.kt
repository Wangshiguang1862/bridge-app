package com.demo.bridge

import android.content.Context
import android.os.Environment
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object BridgeData {

    const val PREFS_NAME = "bridge_prefs"
    const val KEY_COMMANDS = "commands"

    // 私有文件夹
    private fun privateDir(context: Context): File {
        return context.getExternalFilesDir(null)
            ?: File(context.filesDir, "bridge")
    }

    // 公共备份目录（卸载不丢）
    private fun publicBackupDir(): File {
        return File(Environment.getExternalStorageDirectory(), "BridgeBackup")
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ---------- 命令读写 ----------

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
                        enabled = o.optBoolean("enabled", false),
                        type = o.optString("type", "shell")
                    )
                )
            }
        } catch (_: Throwable) {
        }
        return result
    }

    fun saveItems(context: Context, items: List<CommandItem>) {
        val arr = JSONArray()
        items.forEach { item ->
            val o = JSONObject()
            o.put("name", item.name)
            o.put("pkg", item.pkg)
            o.put("action", item.action)
            o.put("enabled", item.enabled)
            o.put("type", item.type)
            arr.put(o)
        }
        prefs(context).edit().putString(KEY_COMMANDS, arr.toString()).apply()
    }

    fun addItem(context: Context, item: CommandItem) {
        val list = loadItems(context).toMutableList()
        list.add(item)
        saveItems(context, list)
    }

    fun removeItem(context: Context, name: String) {
        val list = loadItems(context).filterNot { it.name == name }
        saveItems(context, list)
    }

    fun toJson(items: List<CommandItem>): String {
        val arr = JSONArray()
        items.forEach { item ->
            val o = JSONObject()
            o.put("name", item.name)
            o.put("pkg", item.pkg)
            o.put("action", item.action)
            o.put("enabled", item.enabled)
            o.put("type", item.type)
            arr.put(o)
        }
        return arr.toString()
    }

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
                        enabled = o.optBoolean("enabled", false),
                        type = o.optString("type", "shell")
                    )
                )
            }
            list
        } catch (_: Throwable) {
            null
        }
    }

    // ---------- 备份 / 恢复（全部） ----------

    fun backupAll(context: Context) {
        val priv = privateDir(context)
        if (!priv.exists()) priv.mkdirs()
        File(priv, "scripts").mkdirs()

        publicBackupDir().mkdirs()
        val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val zipFile = File(publicBackupDir(), "bridge_$ts.zip")

        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            // commands.json
            val commandsFile = File(priv, "commands.json")
            if (!commandsFile.exists()) {
                commandsFile.writeText(toJson(loadItems(context)))
            }
            addToZip(zos, commandsFile, priv)

            // scripts/
            val scriptsDir = File(priv, "scripts")
            scriptsDir.listFiles()?.forEach { f ->
                addToZip(zos, f, priv)
            }
        }
    }

    fun restoreAll(context: Context) {
        val pub = publicBackupDir()
        val latest = pub.listFiles { f -> f.name.endsWith(".zip") }
            ?.maxByOrNull { it.lastModified() }
            ?: return

        val priv = privateDir(context)
        priv.mkdirs()

        ZipInputStream(FileInputStream(latest)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val outFile = File(priv, entry.name)
                outFile.parentFile?.mkdirs()
                if (!entry.isDirectory) {
                    FileOutputStream(outFile).use { fos ->
                        zis.copyTo(fos)
                    }
                }
                entry = zis.nextEntry
            }
        }

        // 从解压出的 commands.json 加载
        val commandsFile = File(priv, "commands.json")
        if (commandsFile.exists()) {
            val items = parseItems(commandsFile.readText())
            if (items != null) saveItems(context, items)
        }
    }

    private fun addToZip(zos: ZipOutputStream, file: File, baseDir: File) {
        val entryName = file.relativeTo(baseDir).path
        zos.putNextEntry(ZipEntry(entryName))
        FileInputStream(file).use { fis ->
            fis.copyTo(zos)
        }
        zos.closeEntry()
    }

    // ---------- 单条导入 / 导出 ----------

    fun exportCommand(context: Context, item: CommandItem) {
        val priv = privateDir(context)
        File(priv, "scripts").mkdirs()
        val file = File(priv, "scripts/${item.name}.${if (item.type == "js") "js" else "sh"}")
        file.writeText(item.action)
    }

    fun importFromClipboard(context: Context, text: String, type: String) {
        val name = "imported_${System.currentTimeMillis()}"
        addItem(context, CommandItem(name = name, action = text.trim(), type = type))
    }

    fun importFromFile(context: Context, file: File, type: String) {
        if (!file.exists()) return
        val name = file.nameWithoutExtension
        addItem(context, CommandItem(name = name, action = file.readText(), type = type))
    }
}
