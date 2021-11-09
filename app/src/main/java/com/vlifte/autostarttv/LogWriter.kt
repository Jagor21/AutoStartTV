package com.vlifte.autostarttv

import android.content.Context

object LogWriter {
    const val LOG_FILE_NAME = "logs.txt"
    const val APP_NAME = "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" +
            "\n   ____              ||                     ||                     ||" +
            "\n //    \\\\            ||              |¯¯¯¯  ||        __     ____  ||" +
            "\n||______||  ||   || ====    ||¯¯¯||  |---| ====     //  \\\\  ||    ====" +
            "\n||      ||  ||___||  ||___  ||___||  ____|  ||___  ||¯¯¯¯|| ||     ||___" +
            "\n~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"

    fun log(context: Context, sBody: String) {
        try {
            val fos = context.openFileOutput(LOG_FILE_NAME, Context.MODE_APPEND)
            val strBuilder = StringBuilder()
            strBuilder.append(createLogDateString().plus(": "))
            strBuilder.append("${sBody}\n")
            fos.write(strBuilder.toString().toByteArray())
            fos.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun clearLog(context: Context) {
        try {
            val fos = context.openFileOutput(LOG_FILE_NAME, Context.MODE_PRIVATE)
            val strBuilder = StringBuilder()
            strBuilder.append("${createLogDateString()} Log cleared")
            strBuilder.append("\n")
            fos.write(strBuilder.toString().toByteArray())
            fos.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}