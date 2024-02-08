package com.vlifte.autostarttv

import android.app.Application
import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ConfigUpdate
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.jakewharton.processphoenix.ProcessPhoenix
import dagger.hilt.android.HiltAndroidApp
import org.json.JSONObject


@HiltAndroidApp
class AutostartTvApp : Application() {

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        initFirebaseConfig()
        LogWriter.log(this, "\n${LogWriter.APP_NAME}\n\nAutostartTvApp: app started")
//        Thread.setDefaultUncaughtExceptionHandler { _, _ ->
//            ProcessPhoenix.triggerRebirth(this@AutostartTvApp)
//        }


    }

    private fun initFirebaseConfig() {
        val config = Firebase.remoteConfig
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 0
        }
        config.setConfigSettingsAsync(configSettings)

        config.fetchAndActivate().addOnCompleteListener {
            if (it.isSuccessful) {
                Log.d("FirebaseRemoteConfig", "Remote config fetched successfully")
                val values = config.all
                val json = JSONObject()
                for((key, value) in values) {
                    json.put(key, value.asString())
                }
            } else {
                Log.d("FirebaseRemoteConfig", "FAILURE! Remote config fetched unsuccessfully")
            }
        }
        config.addOnConfigUpdateListener(object : ConfigUpdateListener {
            override fun onUpdate(configUpdate: ConfigUpdate) {
                config.fetchAndActivate()
            }

            override fun onError(error: FirebaseRemoteConfigException) {
                error.printStackTrace()
            }
        })
    }
}
