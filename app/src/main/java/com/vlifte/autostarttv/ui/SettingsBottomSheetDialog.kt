package com.vlifte.autostarttv.ui

import android.content.Context
import android.provider.Settings
import android.util.Log
import android.widget.*
import androidx.annotation.ArrayRes
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.vlifte.autostarttv.*
import com.vlifte.autostarttv.utils.TimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

private const val HOUR_MILLISECONDS = 3600000
private const val MINUTES_MILLISECONDS = 60000

private const val CONNECT_MONITOR_URL = "https://vlifte.by/_tv"
private const val VLIFTE = "vlifte"
private const val VLIFTE_TV = "_tv"
private const val GOOGLE_URL = "https://google.com"
private const val HTTPS = "https"

class SettingsBottomSheetDialog (
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
        val etUrl: EditText? = findViewById(R.id.et_url)

        val spinnerSleepHour: Spinner? = findViewById(R.id.spinner_sleep_hour)
        val spinnerSleepMinute: Spinner? = findViewById(R.id.spinner_sleep_minute)

        val btnSetUrl: MaterialButton? = findViewById(R.id.btn_set_url)
        val btnSetSleepTime: MaterialButton? = findViewById(R.id.btn_set_sleep_time)
        val btnClose: ImageView? = findViewById(R.id.iv_close)

        val btnVlifteUrl: MaterialButton? = findViewById(R.id.btn_vlifte_url)
        val btnGoogleUrl: MaterialButton? = findViewById(R.id.btn_google_url)
        val btnClearToken: MaterialButton? = findViewById(R.id.btn_clear_token)
        val btnClearLogs: MaterialButton? = findViewById(R.id.btn_clear_logs)
        val btnSleepImmediately: MaterialButton? = findViewById(R.id.btn_sleep_immediately)

        createSpinnerAdapter(spinnerSleepHour, R.array.sleep_hours, TimeUtils.sleepHour)
        createSpinnerAdapter(spinnerSleepMinute, R.array.sleep_minutes, TimeUtils.sleepMinute)

        etUrl?.setText(adUrl)

        btnVlifteUrl?.let { btn ->
            btn.setOnClickListener {
                val url = "$CONNECT_MONITOR_URL}/"
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
                setWebViewUrl(UrlData(true, TvWebViewClient.WHITE_NOISE_HTML))
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
                        getTimeFromAppSettings()
                    }
                }
                showToast(context.getString(R.string.sleep_time_set))
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
                setScreenTimeOut(5000)
            } else {
                setScreenTimeOut(TimeUtils.getCurrentTimeMillis())
            }
        }
    }

    fun getAdUrlFromAppSettings() {
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
                setWebViewUrl(UrlData(false, adUrl))
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
                "\nSettingsBottomSheetDialog: onResume() defaultScreenTimeOut = $defaultScreenTimeOut timeSetSuccessfully = $timeSetSuccessfully\nSystem will go sleep in ${currentTimeMillis / HOUR_MILLISECONDS} hours ${currentTimeMillis / MINUTES_MILLISECONDS} minutes\n"
            )
            Log.d(
                "SettingsBottomSheet",
                "\nSettingsBottomSheetDialog: setScreenTimeOut() defaultScreenTimeOut = $defaultScreenTimeOut timeSetSuccessfully = $timeSetSuccessfully\nSystem will go sleep in ${currentTimeMillis / HOUR_MILLISECONDS} hours ${(currentTimeMillis / MINUTES_MILLISECONDS) % 60} minutes\n"
            )
        }
    }
}
