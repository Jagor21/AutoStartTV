package com.vlifte.autostarttv.ui

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.*
import androidx.annotation.ArrayRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.Group
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.vlifte.autostarttv.*
import com.vlifte.autostarttv.TvWebViewClient.Companion.BLR_LOGO_HTML
import com.vlifte.autostarttv.receiver.LockTvReceiver
import com.vlifte.autostarttv.receiver.LockTvReceiver.Companion.ACTION_CLOSE
import com.vlifte.autostarttv.receiver.LockTvReceiver.Companion.REQUEST_SLEEP_CODE
import com.vlifte.autostarttv.receiver.LockTvReceiver.Companion.REQUEST_WAKE_CODE
import com.vlifte.autostarttv.receiver.LockTvReceiver.Companion.SLEEP_REQUEST_CODE
//import com.vlifte.autostarttv.receiver.LockTvReceiver.Companion.WAKE_REQUEST_CODE
import com.vlifte.autostarttv.utils.TimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

private const val HOUR_MILLISECONDS = 3600000
private const val MINUTES_MILLISECONDS = 60000

private const val CONNECT_MONITOR_URL = "https://vlifte.by/_tv"
private const val VLIFTE = "vlifte"
private const val VLIFTE_TV = "_tv"
private const val GOOGLE_URL = "https://google.com"
private const val HTTPS = "https"

class SettingsBottomSheetDialog(
    context: Context,
    private var appSettings: AppSettings,
    private val scope: CoroutineScope
) : BottomSheetDialog(context) {

    private val _webViewUrl = MutableStateFlow(UrlData(false, ""))
    val webViewUrl: StateFlow<UrlData> = _webViewUrl.asStateFlow()

    private var adUrl: String = ""

    private fun setWebViewUrl(data: UrlData) {
        _webViewUrl.value = UrlData(false, "")
        _webViewUrl.value = data
    }

    init {
        setContentView(R.layout.settings_bottom_sheet)
    }

    fun open() {

//        getTimeFromAppSettings()
        val wakeContainer = findViewById<ConstraintLayout>(R.id.wake_time_container)
        val tvWakeTimeTitle = findViewById<TextView>(R.id.tv_wake_time_title)
        val tvSleepTimeTitle = findViewById<TextView>(R.id.tv_sleep_time_title)
        val spinnerWakeHourContainer = findViewById<FrameLayout>(R.id.spinner_wake_hour_container)
        val spinnerWakeMinuteContainer =
            findViewById<FrameLayout>(R.id.spinner_wake_minute_container)
//        val btnSetWakeTime = findViewById<MaterialButton>(R.id.btn_set_wake_time)


        val sleepContainer = findViewById<ConstraintLayout>(R.id.sleep_time_container)

        val androidVersion = Build.VERSION.SDK_INT
        if (androidVersion < Build.VERSION_CODES.R) {
            wakeContainer?.isGone = true
        }

        val etUrl: EditText? = findViewById(R.id.et_url)

        val spinnerSleepHour: Spinner? = findViewById(R.id.spinner_sleep_hour)
        val spinnerSleepMinute: Spinner? = findViewById(R.id.spinner_sleep_minute)
        val spinnerSleepHourContainer: FrameLayout? =
            findViewById(R.id.spinner_sleep_hour_container)
        val spinnerSleepMinuteContainer: FrameLayout? =
            findViewById(R.id.spinner_sleep_minute_container)

        val spinnerWakeHour: Spinner? = findViewById(R.id.spinner_wake_hour)
        val spinnerWakeMinute: Spinner? = findViewById(R.id.spinner_wake_minute)

        val btnSetUrl: MaterialButton? = findViewById(R.id.btn_set_url)
        val btnSetSleepTime: MaterialButton? = findViewById(R.id.btn_set_sleep_time)
        val btnSetWakeTime: MaterialButton? = findViewById(R.id.btn_set_wake_time)
        val btnClose: ImageView? = findViewById(R.id.iv_close)

        val btnVlifteUrl: MaterialButton? = findViewById(R.id.btn_vlifte_url)
        val btnGoogleUrl: MaterialButton? = findViewById(R.id.btn_google_url)
        val btnClearToken: MaterialButton? = findViewById(R.id.btn_clear_token)
        val btnClearLogs: MaterialButton? = findViewById(R.id.btn_clear_logs)
        val btnSleepImmediately: MaterialButton? = findViewById(R.id.btn_sleep_immediately)

        val gTimers = findViewById<Group>(R.id.g_timers)

        when (Build.VERSION.SDK_INT) {
            in Build.VERSION_CODES.BASE..Build.VERSION_CODES.M -> {
                wakeContainer?.isGone = true
                sleepContainer?.isGone = true
                tvSleepTimeTitle?.isGone = true
                spinnerSleepHourContainer?.isGone = true
                spinnerSleepMinuteContainer?.isGone = true
                btnSetSleepTime?.isGone = true

                sleepContainer?.isGone = false
                tvWakeTimeTitle?.isGone = true
                spinnerWakeHourContainer?.isGone = true
                spinnerWakeMinuteContainer?.isGone = true
                btnSetWakeTime?.isGone = true
            }

            Build.VERSION_CODES.P -> {
                tvWakeTimeTitle?.isGone = true
                spinnerWakeHourContainer?.isGone = true
                spinnerWakeMinuteContainer?.isGone = true
                btnSetWakeTime?.isGone = true
            }

            in Build.VERSION_CODES.Q..Build.VERSION_CODES.TIRAMISU -> {

            }

        }

//        if(Build.VERSION.SDK_INT == Build.VERSION_CODES.P) {
//            sleepContainer?.isGone = false
//            tvWakeTimeTitle?.isGone = true
//            spinnerWakeHourContainer?.isGone = true
//            spinnerWakeMinuteContainer?.isGone = true
//            btnSetWakeTime?.isGone = true
//        }
//        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
//            wakeContainer?.isGone = true
//            sleepContainer?.isGone = true
//            tvSleepTimeTitle?.isGone = true
//            spinnerSleepHourContainer?.isGone = true
//            spinnerSleepMinuteContainer?.isGone = true
//            btnSetSleepTime?.isGone = true
//
//            sleepContainer?.isGone = false
//            tvWakeTimeTitle?.isGone = true
//            spinnerWakeHourContainer?.isGone = true
//            spinnerWakeMinuteContainer?.isGone = true
//            btnSetWakeTime?.isGone = true
//        }


        launch {
            appSettings.saved.collect {

                createSpinnerAdapter(spinnerSleepHour, R.array.sleep_hours, it.sleepHour)
                createSpinnerAdapter(spinnerSleepMinute, R.array.sleep_minutes, it.sleepMinute)

                createSpinnerAdapter(spinnerWakeHour, R.array.sleep_hours, it.wakeUpHour)
                createSpinnerAdapter(spinnerWakeMinute, R.array.sleep_minutes, it.wakeUpMinute)
            }
        }

        etUrl?.setText(adUrl)

        btnVlifteUrl?.let { btn ->
            btn.setOnClickListener {
                val url = "$CONNECT_MONITOR_URL/"
                etUrl?.setText(url)
            }
        }

        btnGoogleUrl?.let { btn ->
            btn.setOnClickListener {
                etUrl?.setText(GOOGLE_URL)
            }
        }

        btnClearToken?.let { btn ->
            btn.setOnClickListener {
                launch {
                    appSettings.setDeviceToken("")
                    showToast(context.getString(R.string.token_cleared))
                }
            }
        }

        btnClearLogs?.let { btn ->
            btn.setOnClickListener {
                LogWriter.clearLog(context)
                showToast(context.getString(R.string.log_cleared))
            }
        }

        btnSleepImmediately?.let { btn ->
            btn.setOnClickListener {
                setWebViewUrl(UrlData(true, TvWebViewClient.BLR_LOGO_HTML))
                getTimeFromAppSettings(true)
                dismiss()
            }
        }

        btnSetUrl?.let {
            it.setOnClickListener {
                var newAdUrl = etUrl!!.text.toString()
                if (newAdUrl.isNotEmpty()) {
                    etUrl.error = null

                    if (!newAdUrl.contains(HTTPS)) {
                        newAdUrl = context.getString(R.string.https, newAdUrl)
                    }
                    launch {

                        if (newAdUrl.contains(VLIFTE_TV)) {
                            appSettings.saved.collect {
                                newAdUrl = "$CONNECT_MONITOR_URL/${it.deviceToken}"
                                adUrl = newAdUrl
                                appSettings.setAdUrl(newAdUrl)
                                setWebViewUrl(UrlData(false, newAdUrl))
                            }
                            return@launch
                        }
                        adUrl = newAdUrl
                        appSettings.setAdUrl(newAdUrl)
                        setWebViewUrl(UrlData(false, newAdUrl))
                    }
                    showToast(context.getString(R.string.url_changed))

                } else {
                    etUrl.error = context.getString(R.string.url_is_blank)
                }
            }
        }

        btnSetSleepTime?.let {
            it.setOnClickListener {
                val sleepHour = spinnerSleepHour!!.selectedItem.toString().toInt()
                val sleepMinute = spinnerSleepMinute!!.selectedItem.toString().toInt()

                launch {
                    appSettings.apply {
                        setSleepHour(sleepHour)
                        setSleepMinute(sleepMinute)
                        setAlarm(sleepHour, sleepMinute, SLEEP_REQUEST_CODE, REQUEST_SLEEP_CODE)
                        TimeUtils.sleepHour = sleepHour
                        TimeUtils.sleepMinute = sleepMinute
//                        getTimeFromAppSettings()
                    }
                }
                showToast(context.getString(R.string.sleep_time_set))
            }
        }
        btnSetWakeTime?.let {
            it.setOnClickListener {
                val wakeHour = spinnerWakeHour!!.selectedItem.toString().toInt()
                val wakeMinute = spinnerWakeMinute!!.selectedItem.toString().toInt()

                launch {
                    appSettings.apply {
                        setWakeUpHour(wakeHour)
                        setWakeUpMinute(wakeMinute)
                        setAlarm(wakeHour, wakeMinute, SLEEP_REQUEST_CODE, REQUEST_WAKE_CODE)
                        TimeUtils.wakeHour = wakeHour
                        TimeUtils.wakeMinute = wakeMinute
                    }
                }
                showToast(context.getString(R.string.wake_time_set))
            }
        }

        btnClose?.let {
            it.setOnClickListener {
                dismiss()
            }
        }
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        show()
    }

    private fun setAlarm(
        sleepHour: Int = TimeUtils.sleepHour,
        sleepMinute: Int = TimeUtils.sleepMinute,
        requestCodeName: String,
        requestCode: Int
    ) {
        val intent = Intent(context, LockTvReceiver::class.java)
        intent.action = ACTION_CLOSE
        val pendingIntent = PendingIntent.getBroadcast(
            context.applicationContext,
            requestCode,
            intent.putExtra(requestCodeName, requestCode),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        )

        var alarmManager: AlarmManager =
            ContextCompat.getSystemService(context, AlarmManager::class.java) as AlarmManager
        alarmManager.cancel(pendingIntent)
        val calendar: Calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, sleepHour)
            set(Calendar.MINUTE, sleepMinute)
            set(Calendar.SECOND, 0)
        }

        if (calendar.before(Calendar.getInstance())) {
            calendar.add(Calendar.DATE, 1)
        }

        val dateFormat = SimpleDateFormat("dd MM yyyy hh:mm:ss")

        LogWriter.log(
            context,
            "SettingsBottomSheetDialog: setting lock alarm for $sleepHour:$sleepMinute ${
                dateFormat.format(calendar.time)
            }"
        )
        Log.d(
            "SettingsBottomDialog",
            "SettingsBottomSheetDialog: setting lock alarm for $sleepHour:$sleepMinute ${
                dateFormat.format(
                    calendar.time
                )
            }"
        )
//        alarmManager.setRepeating(
//            AlarmManager.RTC_WAKEUP,
//            calendar.timeInMillis,
//            AlarmManager.INTERVAL_DAY,
//            pendingIntent
//        )
        try {
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(calendar.timeInMillis, pendingIntent), pendingIntent
            )
        } catch (e: SecurityException) {
            e.localizedMessage?.let { Log.d("Set alarm", it) }
        }
//        alarmManager.set(
//            AlarmManager.RTC_WAKEUP,
//            calendar.timeInMillis,
//            pendingIntent
//        )
//        if (Settings.System.canWrite(context)){

        LogWriter.log(
            context,
            "TvActivity: SCREEN_OFF_TIMEOUT set to ${
                Settings.System.getInt(
                    context.contentResolver,
                    Settings.System.SCREEN_OFF_TIMEOUT
                )
            }"
        )
        Settings.System.putInt(
            context.contentResolver,
            Settings.System.SCREEN_OFF_TIMEOUT, (24 * 3600000)
        )
//        } else {
//            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
//            intent.data = Uri.parse("package:" + context.packageName)
//            context.startActivity(intent)
//        }
    }


    fun getTimeFromAppSettings(immediately: Boolean = false) {
        launch {
            appSettings.saved.collect { settings ->
                TimeUtils.sleepHour = if (settings.sleepHour != 0) {
                    settings.sleepHour
                } else {
                    appSettings.setSleepHour(TimeUtils.DEFAULT_SLEEP_HOUR)
                    TimeUtils.DEFAULT_SLEEP_HOUR
                }
                TimeUtils.sleepMinute = settings.sleepMinute
            }
            if (immediately) {
//                setScreenTimeOut(5000)
            } else {
                setAlarm(requestCodeName = SLEEP_REQUEST_CODE, requestCode = REQUEST_SLEEP_CODE)
//                setScreenTimeOut(TimeUtils.getCurrentTimeMillis())
            }
        }
    }

    fun getAdUrlFromAppSettings(isBase: Boolean = false) {
        launch {
            appSettings.saved.collect { settings ->
                adUrl = when {
                    settings.adUrl.contains(VLIFTE) -> "$CONNECT_MONITOR_URL/${settings.deviceToken}"
                    settings.adUrl.isEmpty() -> {
                        appSettings.setAdUrl(CONNECT_MONITOR_URL)
                        CONNECT_MONITOR_URL
                    }

                    else -> settings.adUrl
                }
                if (isBase) {
                    setWebViewUrl(UrlData(true, BLR_LOGO_HTML))
                    Settings.System.putInt(
                        context.contentResolver,
                        Settings.System.SCREEN_OFF_TIMEOUT, (5000)
                    )
                } else {
                    setWebViewUrl(UrlData(false, adUrl))
                }
            }
        }
    }

    private fun createSpinnerAdapter(
        spinner: Spinner?,
        @ArrayRes stringArrayRes: Int,
        itemSelection: Int
    ) {
        ArrayAdapter.createFromResource(
            context,
            stringArrayRes,
            android.R.layout.simple_spinner_item
        ).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner?.adapter = it
            var item = itemSelection.toString()
            if (item.length == 1) item = "0$item"
            val position = it.getPosition(item)
            spinner?.setSelection(position)
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    private fun launch(block: suspend () -> Unit) {
        scope.launch {
            block()
        }
    }

    private fun setScreenTimeOut(currentTimeMillis: Int) {
        val settingsCanWrite = Settings.System.canWrite(context)
        if (!settingsCanWrite) {
            Toast.makeText(
                context,
                "Разрешите изменения системных настроек!\nНастройки -> Приложения -> Специальный доступ -> Изменение системных настроек",
                Toast.LENGTH_LONG
            ).show()
        } else {
            val defaultScreenTimeOut =
                Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_OFF_TIMEOUT)
            val timeSetSuccessfully = Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_OFF_TIMEOUT, (currentTimeMillis)
            )

            LogWriter.log(
                context,
                "\nSettingsBottomSheetDialog: onResume() defaultScreenTimeOut = $defaultScreenTimeOut timeSetSuccessfully = $timeSetSuccessfully\nSystem will go sleep in ${currentTimeMillis / HOUR_MILLISECONDS} hours ${currentTimeMillis / MINUTES_MILLISECONDS % 60} minutes\n"
            )
            Log.d(
                "SettingsBottomSheet",
                "\nSettingsBottomSheetDialog: setScreenTimeOut() defaultScreenTimeOut = $defaultScreenTimeOut timeSetSuccessfully = $timeSetSuccessfully\nSystem will go sleep in ${currentTimeMillis / HOUR_MILLISECONDS} hours ${(currentTimeMillis / MINUTES_MILLISECONDS) % 60} minutes\n"
            )
        }
    }
}
