package com.vlifte.autostarttv.receiver

import android.content.*
import android.provider.Settings
import android.util.Log
import com.vlifte.autostarttv.utils.TimeUtils
import android.widget.Toast
import com.vlifte.autostarttv.LogWriter
import com.vlifte.autostarttv.ui.TvActivity
import java.util.*

class LockTvReceiver : BroadcastReceiver() {
    companion object {
        const val REQUEST_LOCK_CODE = 12345
        const val ACTION_CLOSE = "close_action"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent!!.extras?.get("SLEEP_REQUEST_CODE") == REQUEST_LOCK_CODE) {
            context?.let {
                val calendar = Calendar.getInstance()
                val minuteRange = TimeUtils.sleepMinute..TimeUtils.sleepMinute + 3
                Log.d(
                    "LockTvReceiver",
                    "current minute ${calendar.get(Calendar.MINUTE)}\n in range = ${
                        calendar.get(Calendar.MINUTE) in minuteRange
                    }"
                )
                LogWriter.log(
                    context,
                    "LockTvReceiver: current minute ${calendar.get(Calendar.MINUTE)}\n in range = ${
                        calendar.get(Calendar.MINUTE) in minuteRange
                    }"
                )
                if (
                    calendar.get(Calendar.HOUR_OF_DAY) == TimeUtils.sleepHour &&
                    calendar.get(Calendar.MINUTE) in minuteRange
                ) {
                    Toast.makeText(it, "Going to sleep", Toast.LENGTH_LONG).show()
                    LogWriter.log(
                        context,
                        "\nLockTvReceiver: onReceive() go to sleep\n"
                    )
                    Log.d(
                        "LockTvReceiver",
                        "\nLockTvReceiver: onReceive() go to sleep\n"
                    )

                    val intent = Intent(context, TvActivity::class.java)
                    intent.apply{
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        putExtra(ACTION_CLOSE, true)
                    }
                    context.applicationContext.startActivity(intent)

                        Settings.System.putInt(
                            context.contentResolver,
                            Settings.System.SCREEN_OFF_TIMEOUT, (10000)
                        )



                    LogWriter.log(
                        context,
                        "TvActivity: SCREEN_OFF_TIMEOUT set to ${
                            Settings.System.getInt(
                                context.contentResolver,
                                Settings.System.SCREEN_OFF_TIMEOUT
                            )
                        }"
                    )
                }
            }
        }
    }
}