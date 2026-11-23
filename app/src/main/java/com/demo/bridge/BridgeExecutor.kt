package com.demo.bridge

import android.content.Context
import android.widget.Toast

object BridgeExecutor {
    fun testExecute(ctx: Context, item: MainActivity.CommandItem) {
        // 第一步：只做 Toast 占位，保证列表点“测试执行”不崩
        // 第二步：接 Shizuku.newProcess / Shell 真正执行 action
        Toast.makeText(ctx, "测试执行：${item.name}\n${item.action}", Toast.LENGTH_SHORT).show()
    }
}
