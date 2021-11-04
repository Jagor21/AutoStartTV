package com.vlifte.autostarttv

import android.content.*
import android.os.Build
import android.util.Log

class ScreenStateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        val i = Intent(context, BootUpService::class.java)
        Log.d("ScreenStateReceiver", "ScreenStateReceiver: ${intent?.action}")
        context?.let {
            LogWriter.log(
                it,
                "ScreenStateReceiver: Starting MyService\nIntent action: ${intent?.action}"
            )
        }
        if (intent?.action == Intent.ACTION_SCREEN_ON) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context?.startForegroundService(i)
            } else {
                context?.startService(i)
            }
        }
        Log.i("AutostartTV", "started")
    }
}