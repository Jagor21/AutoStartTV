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
}