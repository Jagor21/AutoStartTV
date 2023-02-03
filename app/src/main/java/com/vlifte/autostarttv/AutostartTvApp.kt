package com.vlifte.autostarttv

import android.app.Application
import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.jakewharton.processphoenix.ProcessPhoenix
import dagger.hilt.android.HiltAndroidApp


@HiltAndroidApp
class AutostartTvApp : Application() {

    override fun onCreate() {
        super.onCreate()
        LogWriter.log(this, "\n${LogWriter.APP_NAME}\n\nAutostartTvApp: app started")
//        Thread.setDefaultUncaughtExceptionHandler { _, _ ->
//            ProcessPhoenix.triggerRebirth(this@AutostartTvApp)
//        }
    }
}
