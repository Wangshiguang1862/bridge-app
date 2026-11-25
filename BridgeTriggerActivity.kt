package com.demo.bridge

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast

class BridgeTriggerActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 无界面，直接执行后退出
        val items = BridgeData.loadItems(this)
        if (items.isEmpty()) {
            Toast.makeText(this, "暂无命令", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // 默认执行第一条 enabled=true 的命令
        val target = items.firstOrNull { it.enabled }
        if (target == null) {
            Toast.makeText(this, "没有已启用的命令", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        Thread {
            val result = BridgeExecutor.execByType(target.type, target.action)
            runOnUiThread {
                // 可选：显示执行结果，不想弹就删掉这两行
                Toast.makeText(this, "Bridge: ${target.name} 执行完成", Toast.LENGTH_SHORT).show()
                finish()
            }
        }.start()
    }
}
