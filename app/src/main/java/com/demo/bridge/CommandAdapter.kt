package com.demo.bridge

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast

class CommandAdapter(
    private val context: Context,
    private val items: MutableList<CommandItem>
) : BaseAdapter() {

    override fun getCount(): Int = items.size
    override fun getItem(position: Int): CommandItem = items[position]
    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_command, parent, false)

        val item = getItem(position)
        val tvSeq = view.findViewById<TextView>(R.id.tvSeq)
        val tvName = view.findViewById<TextView>(R.id.tvName)
        val cbEnable = view.findViewById<CheckBox>(R.id.cbEnable)

        tvSeq.text = item.seq.toString()
        tvName.text = item.file.nameWithoutExtension
        cbEnable.isChecked = isEnabled(item.file)

        cbEnable.setOnCheckedChangeListener { _, isChecked ->
            setEnabled(item.file, isChecked)
        }

        view.setOnClickListener { showMenu(item) }
        return view
    }

    private fun prefKey(file: File) = "cmd_enable_${file.name}"

    private fun isEnabled(file: File): Boolean {
        val prefs = context.getSharedPreferences("commands", Context.MODE_PRIVATE)
        return prefs.getBoolean(prefKey(file), true)
    }

    private fun setEnabled(file: File, enabled: Boolean) {
        context.getSharedPreferences("commands", Context.MODE_PRIVATE)
            .edit().putBoolean(prefKey(file), enabled).apply()
    }

    private fun showMenu(item: CommandItem) {
        val options = arrayOf("编辑", "调试", "导出", "删除")
        AlertDialog.Builder(context)
            .setTitle(item.file.nameWithoutExtension)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> edit(item)
                    1 -> debug(item)
                    2 -> export(item)
                    3 -> delete(item)
                }
            }.show()
    }

    private fun edit(item: CommandItem) {
        val intent = Intent(context, EditCommandActivity::class.java).apply {
            putExtra("path", item.file.absolutePath)
        }
        context.startActivity(intent)
    }

    private fun debug(item: CommandItem) {
        val result = BridgeService().doExecute(item.file.readText().trim())
        Toast.makeText(context, result, Toast.LENGTH_LONG).show()
    }

    private fun export(item: CommandItem) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_STREAM, Uri.fromFile(item.file))
        }
        context.startActivity(Intent.createChooser(intent, "导出命令"))
    }

    private fun delete(item: CommandItem) {
        AlertDialog.Builder(context)
            .setTitle("删除命令")
            .setMessage("确定删除 ${item.file.name}？")
            .setPositiveButton("删除") { _, _ ->
                item.file.delete()
                items.remove(item)
                notifyDataSetChanged()
            }
            .setNegativeButton("取消", null)
            .show()
    }
}
