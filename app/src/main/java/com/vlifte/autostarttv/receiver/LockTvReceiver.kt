package com.vlifte.autostarttv.receiver

import android.content.*
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.vlifte.autostarttv.utils.TimeUtils
import android.widget.Toast
import com.vlifte.autostarttv.LockScreenCodeEvent
import com.vlifte.autostarttv.LogWriter
import com.vlifte.autostarttv.ui.TvActivity
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.greenrobot.eventbus.EventBus
import java.util.*

class LockTvReceiver : BroadcastReceiver() {
    companion object {
        const val REQUEST_SLEEP_CODE = 12345
        const val REQUEST_WAKE_CODE = 67890
        const val SLEEP_REQUEST_CODE = "SLEEP_REQUEST_CODE"

        //        const val WAKE_REQUEST_CODE = "SLEEP_REQUEST_CODE"
        const val ACTION_CLOSE = "close_action"
        const val ACTION_BLACK_SCREEN = "black_screen"

        private val myEvent = MutableSharedFlow<LockScreenCodeEvent>(
            replay = 1,
            onBufferOverflow = BufferOverflow.DROP_LATEST
        )
        val event = myEvent.asSharedFlow()
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        val androidVersion = Build.VERSION.SDK_INT
        intent?.let { i ->            val code = i.extras?.getInt(SLEEP_REQUEST_CODE)
            myEvent.tryEmit(
                if (androidVersion < Build.VERSION_CODES.R) {
                    LockScreenCodeEvent.EVENT_CLOSE
                } else {
                    if (code == REQUEST_WAKE_CODE) LockScreenCodeEvent.EVENT_BLACK_SCREEN_OFF else LockScreenCodeEvent.EVENT_BLACK_SCREEN
                }
            )
//            if (androidVersion < Build.VERSION_CODES.R) {
//                if (code == REQUEST_SLEEP_CODE) {
//                    EventBus.getDefault().post(LockScreenCodeEvent.EVENT_CLOSE)
//                }
//            } else {
//                EventBus.getDefault().post(LockScreenCodeEvent.EVENT_BLACK_SCREEN(!code == REQUEST_WAKE_CODE))
//            }

        }
//        if (androidVersion < Build.VERSION_CODES.R) {
//            if (intent!!.extras?.getInt(SLEEP_REQUEST_CODE) == REQUEST_SLEEP_CODE) {
//                context?.let {
//                    val calendar = Calendar.getInstance()
//                    val minuteRange = TimeUtils.sleepMinute..TimeUtils.sleepMinute + 3
//                    Log.d(
//                        "LockTvReceiver",
//                        "current minute ${calendar.get(Calendar.MINUTE)}\n in range = ${
//                            calendar.get(Calendar.MINUTE) in minuteRange
//                        }"
//                    )
//                    LogWriter.log(
//                        context,
//                        "LockTvReceiver: current minute ${calendar.get(Calendar.MINUTE)}\n in range = ${
//                            calendar.get(Calendar.MINUTE) in minuteRange
//                        }"
//                    )
//                    if (
//                        calendar.get(Calendar.HOUR_OF_DAY) == TimeUtils.sleepHour &&
//                        calendar.get(Calendar.MINUTE) in minuteRange
//                    ) {
//                        Toast.makeText(it, "Going to sleep", Toast.LENGTH_LONG).show()
//                        LogWriter.log(
//                            context,
//                            "\nLockTvReceiver: onReceive() go to sleep\n"
//                        )
//                        Log.d(
//                            "LockTvReceiver",
//                            "\nLockTvReceiver: onReceive() go to sleep\n"
//                        )
//
//                        val i = Intent(context, TvActivity::class.java)
//                        i.apply {
//                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
//                            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
//                            putExtra(ACTION_CLOSE, true)
//                        }
//                        context.applicationContext.startActivity(i)
//                        Settings.System.putInt(
//                            context.contentResolver,
//                            Settings.System.SCREEN_OFF_TIMEOUT, (10000)
//                        )
//
//
//
//                        LogWriter.log(
//                            context,
//                            "TvActivity: SCREEN_OFF_TIMEOUT set to ${
//                                Settings.System.getInt(
//                                    context.contentResolver,
//                                    Settings.System.SCREEN_OFF_TIMEOUT
//                                )
//                            }"
//                        )
//                    }
//                }
//            }
//        } else {
//            if (intent!!.extras?.getInt(WAKE_REQUEST_CODE) == REQUEST_WAKE_CODE) {
//                val i = Intent(context, TvActivity::class.java)
//                i.apply {
//                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
//                    addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
//                    putExtra(ACTION_BLACK_SCREEN, false)
//                }
//                context?.applicationContext?.startActivity(i)
//            } else {
//                val i = Intent(context, TvActivity::class.java)
//                i.apply {
//                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
////                    addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
//                    putExtra(ACTION_BLACK_SCREEN, true)
//                }
//                context?.applicationContext?.startActivity(i)
//            }
//        }

    }
}