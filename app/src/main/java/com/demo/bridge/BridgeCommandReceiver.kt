package com.demo.bridge

import android.app.Activity
import android.content.Intent
import android.os.Bundle

class BridgeCommandReceiver : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val action = intent?.action
        if (action == "com.demo.bridge.EXECUTE") {
            val name = intent.getStringExtra("name") ?: ""
            val pkg = intent.getStringExtra("pkg") ?: ""
            val cmd = intent.getStringExtra("action") ?: ""
            if (cmd.isNotEmpty()) {
                val item = MainActivity.CommandItem(name, pkg, cmd, true)
                val log = CommandRunner.run(this, item, false)
                val result = Intent().apply {
                    putExtra("ret", log.retCode)
                    putExtra("out", log.stdout)
                    putExtra("err", log.stderr)
                }
                setResult(RESULT_OK, result)
            } else {
                setResult(RESULT_CANCELED, Intent().apply { putExtra("err", "action empty") })
            }
        } else {
            setResult(RESULT_CANCELED)
        }
        finish()
    }
}
