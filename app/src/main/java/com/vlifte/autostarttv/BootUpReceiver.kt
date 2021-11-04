package com.vlifte.autostarttv

import android.content.*
import android.os.Build
import android.util.Log

class BootUpReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val i = Intent(context, BootUpService::class.java)
        Log.d("MY_TAG", "BootUpReceiver: ${intent?.action}" )
        context?.let { LogWriter.log(it, "BootUpReceiver: Starting MyService\nIntent action: ${intent?.action}") }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context?.startForegroundService(i)
        } else {
            context?.startService(i)
        }
        Log.i("AutostartTV", "started")
    }
}