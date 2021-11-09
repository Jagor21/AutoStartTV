package com.vlifte.autostarttv.receiver

import android.content.*
import android.os.Build
import android.util.Log
import com.vlifte.autostarttv.service.BootUpService
import com.vlifte.autostarttv.LogWriter

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

//class ScreenStateService : Service() {
//
//    lateinit var screenStateReceiver: ScreenStateReceiver
//
//    override fun onCreate() {
//        super.onCreate()
//
//        screenStateReceiver = ScreenStateReceiver()
//
//
//    }
//
//    override fun onBind(intent: Intent?): IBinder? {
//        return null
//    }
//
//    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
//        val screenStateFilter = IntentFilter()
//        screenStateFilter.addAction(Intent.ACTION_SCREEN_ON)
//        screenStateFilter.addAction(Intent.ACTION_SCREEN_OFF)
//        registerReceiver(screenStateReceiver, screenStateFilter)
//        return Service.START_STICKY_COMPATIBILITY
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        Log.d("ScreenStateReceiver", "unregisterReceiver(screenStateReceiver)" )
//        unregisterReceiver(screenStateReceiver)
//    }
//}