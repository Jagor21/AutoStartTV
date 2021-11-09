package com.vlifte.autostarttv

import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.take
import javax.inject.Inject

class AppSettings @Inject constructor(val settings: DataStore<ProtoSettings>) {

    companion object {
        const val DEVICE_TOKEN = "device_token"
    }

    val saved = settings.data.take(1)

    suspend fun setDeviceToken(deviceToken: String) = settings.updateData {
        it.toBuilder().setDeviceToken(deviceToken).build()
    }

    suspend fun setSleepHour(sleepHour: Int) = settings.updateData {
        it.toBuilder().setSleepHour(sleepHour).build()
    }

    suspend fun setSleepMinute(sleepMinute: Int) = settings.updateData {
        it.toBuilder().setSleepMinute(sleepMinute).build()
    }

    suspend fun setWakeUpHour(wakeUpHour: Int) = settings.updateData {
        it.toBuilder().setWakeUpHour(wakeUpHour).build()
    }

    suspend fun setWakeUpMinute(wakeUpMinute: Int) = settings.updateData {
        it.toBuilder().setWakeUpMinute(wakeUpMinute).build()
    }

    suspend fun setAdUrl(adUrl: String) = settings.updateData {
        it.toBuilder().setAdUrl(adUrl).build()
    }
}