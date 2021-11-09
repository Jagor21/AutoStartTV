package com.vlifte.autostarttv.utils

import java.util.*

private const val HOUR_MILLISECONDS = 3600000
private const val MINUTES_MILLISECONDS = 60000

object TimeUtils {

    var sleepHour: Int = 0
    var sleepMinute: Int = 0

    const val DEFAULT_SLEEP_HOUR = 23

    fun getCurrentTimeMillis(): Int {
        val calendar = Calendar.getInstance()
        val hourOfDay = calendar.get(Calendar.HOUR_OF_DAY)
        val hour = if (hourOfDay == 0) 24 else hourOfDay
        val minute = calendar.get(Calendar.MINUTE)

        val currentTimeMillis = (hour * HOUR_MILLISECONDS) + (minute * MINUTES_MILLISECONDS)
        val sleepTimeMillis = (sleepHour * HOUR_MILLISECONDS) + (sleepMinute * MINUTES_MILLISECONDS)
        return if (sleepTimeMillis - currentTimeMillis <= 0) 60000 else sleepTimeMillis - currentTimeMillis
    }
}