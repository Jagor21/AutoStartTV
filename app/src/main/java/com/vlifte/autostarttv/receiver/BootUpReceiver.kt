package com.vlifte.autostarttv.receiver

import android.content.*
import android.os.Build
import android.util.Log
import com.vlifte.autostarttv.LogWriter
import com.vlifte.autostarttv.service.BootUpService

class BootUpReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val i = Intent(context, BootUpService::class.java)
        Log.d("BootUpReceiver", "BootUpReceiver: ${intent?.action}" )
        context?.let {
            LogWriter.log(
                it,
                "BootUpReceiver: Starting MyService\nIntent action: ${intent?.action}"
            )
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context?.startForegroundService(i)
        } else {
            context?.startService(i)
        }
        Log.i("AutostartTV", "started")
    }
}