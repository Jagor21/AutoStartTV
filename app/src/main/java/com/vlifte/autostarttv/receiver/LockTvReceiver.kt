package com.vlifte.autostarttv.receiver

import android.content.*
import android.provider.Settings
import android.util.Log
import com.vlifte.autostarttv.LogWriter

class LockTvReceiver : BroadcastReceiver() {
    companion object {
        const val REQUEST_LOCK_CODE = 12345
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent!!.extras?.get("SLEEP_REQUEST_CODE") == REQUEST_LOCK_CODE) {
            if (context != null) {
            val timeSetSuccessfully = Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_OFF_TIMEOUT, (5000)
            )

                LogWriter.log(
                    context,
                    "\nLockTvReceiver: onReceive() go to sleep\n"
                )

                Log.d(
                    "LockTvReceiver",
                    "\nLockTvReceiver: onReceive() go to sleep\n"
                )
            }
        }
    }
}