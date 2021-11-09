package com.vlifte.autostarttv

import java.text.SimpleDateFormat
import java.util.*

private const val LOG_DATE_FORMAT = "yyyy.MM.dd HH:mm:ssss"

fun createLogDateString(): String {
    val sdf = SimpleDateFormat(LOG_DATE_FORMAT, Locale.getDefault())
    val date = Date(System.currentTimeMillis())
    return sdf.format(date)
}