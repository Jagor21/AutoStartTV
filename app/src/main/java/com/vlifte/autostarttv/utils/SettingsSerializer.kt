package com.vlifte.autostarttv.utils

import android.annotation.SuppressLint
import android.util.Log
import androidx.datastore.core.Serializer
import com.google.protobuf.InvalidProtocolBufferException
import com.vlifte.autostarttv.ProtoSettings
import java.io.InputStream
import java.io.OutputStream

private const val TAG = "SETTINGS"

@Suppress("BlockingMethodInNonBlockingContext")
object SettingsSerializer : Serializer<ProtoSettings> {
    override val defaultValue: ProtoSettings = ProtoSettings.getDefaultInstance()

    @SuppressLint("LogNotTimber")
    override suspend fun readFrom(input: InputStream): ProtoSettings {
        return try {
            ProtoSettings.parseFrom(input)
        } catch (e: InvalidProtocolBufferException) {
            Log.e(
                TAG,
                "Cannot read proto. Create default\n${e.unwrapIOException().localizedMessage}"
            )
            defaultValue
        }
    }

    override suspend fun writeTo(t: ProtoSettings, output: OutputStream) = t.writeTo(output)
}