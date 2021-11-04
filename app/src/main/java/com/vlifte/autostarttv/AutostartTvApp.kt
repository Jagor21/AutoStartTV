package com.vlifte.autostarttv

import android.app.Application
import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.util.Log
import dagger.hilt.android.HiltAndroidApp


@HiltAndroidApp
class AutostartTvApp : Application() {

    override fun onCreate() {
        super.onCreate()

//        val screenStateServiceIntent = Intent(this, ScreenStateService::class.java)
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            startForegroundService(screenStateServiceIntent)
//        } else {
//            startService(screenStateServiceIntent)
//        }
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