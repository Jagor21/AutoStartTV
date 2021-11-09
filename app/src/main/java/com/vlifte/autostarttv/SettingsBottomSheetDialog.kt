package com.vlifte.autostarttv

import android.content.Context
import android.provider.Settings
import android.util.Log
import android.widget.*
import androidx.annotation.ArrayRes
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

private const val HOUR_MILLISECONDS = 3600000
private const val MINUTES_MILLISECONDS = 60000

private const val DEFAULT_SLEEP_HOUR = 23

private const val CONNECT_MONITOR_URL = "https://vlifte.by/_tv"
private const val VLIFTE = "vlifte"
private const val VLIFTE_TV = "_tv"

class SettingsBottomSheetDialog(
    context: Context,
    private val scope: CoroutineScope
) : BottomSheetDialog(context) {

    @Inject
    lateinit var appSettings: AppSettings

    private var sleepHour: Int = 0
    private var sleepMinute: Int = 0
    private var adUrl: String = ""

    private val _webViewUrl = MutableStateFlow(UrlData(false, ""))
    val webViewUrl: StateFlow<UrlData> = _webViewUrl.asStateFlow()

    private fun setWebViewUrl(data: UrlData) {
        _webViewUrl.value = data
    }

    init {
        setContentView(R.layout.settings_bottom_sheet)

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

        createSpinnerAdapter(spinnerSleepHour, R.array.sleep_hours, sleepHour)
        createSpinnerAdapter(spinnerSleepMinute, R.array.sleep_minutes, sleepMinute)

        etUrl?.setText(adUrl)

        btnVlifteUrl?.let { btn ->
            btn.setOnClickListener {
                etUrl?.setText("https://vlifte.by/_tv/")
            }
        }

        btnGoogleUrl?.let { btn ->
            btn.setOnClickListener {
                etUrl?.setText("https://google.com")
            }
        }

        btnClearToken?.let { btn ->
            btn.setOnClickListener {
                launch {
                    appSettings.setDeviceToken("")
                    showToast("Token was cleared!")
                }
            }
        }

        btnClearLogs?.let { btn ->
            btn.setOnClickListener {
                LogWriter.clearLog(context)
                showToast("Log was cleared!")
            }
        }

        btnSleepImmediately?.let { btn ->
            btn.setOnClickListener {
                val sHtmlTemplate =
                    "<html><head></head><body><img style=\"width: 100%; height: auto\" src=\"file:///android_asset/white_noise_gif.gif\"></body></html>"
                setWebViewUrl(UrlData(true, sHtmlTemplate))
//                webView.loadDataWithBaseURL(null, sHtmlTemplate, "text/html", "utf-8", null)
                getTimeFromAppSettings(true)
                dismiss()
            }
        }

        btnSetUrl?.let {
            it.setOnClickListener {
                var newAdUrl = etUrl!!.text.toString()
                if (newAdUrl.isNotEmpty()) {
                    etUrl.error = null

                    if (!newAdUrl.contains("https")) {
                        newAdUrl = "https://$newAdUrl"
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
                    showToast("Url successfully changed!")

                } else {
                    etUrl.error = "Url should not be blank!"
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
                showToast("Sleep time successfully changed!")
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

    private fun getTimeFromAppSettings(immediately: Boolean = false) {
        launch {
            appSettings.saved.collect { settings ->
                sleepHour = if (settings.sleepHour != 0) {
                    settings.sleepHour
                } else {
                    appSettings.setSleepHour(DEFAULT_SLEEP_HOUR)
                    DEFAULT_SLEEP_HOUR
                }
                sleepMinute = settings.sleepMinute
            }
            if (immediately) {
                setScreenTimeOut(5000)
            } else {
                setScreenTimeOut(getCurrentTimeMillis())
            }
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
                "\nMainActivity: onResume() defaultScreenTimeOut = $defaultScreenTimeOut timeSetSuccessfully = $timeSetSuccessfully\nSystem will go sleep in ${currentTimeMillis / HOUR_MILLISECONDS} hours ${currentTimeMillis / MINUTES_MILLISECONDS} minutes\n"
            )
            Log.d(
                "MY_TAG",
                "\nMainActivity: onResume() defaultScreenTimeOut = $defaultScreenTimeOut timeSetSuccessfully = $timeSetSuccessfully\nSystem will go sleep in ${currentTimeMillis / HOUR_MILLISECONDS} hours ${(currentTimeMillis / MINUTES_MILLISECONDS) % 60} minutes\n"
            )
        }
    }

    private fun getCurrentTimeMillis(): Int {
        val calendar = Calendar.getInstance()
        val hourOfDay = calendar.get(Calendar.HOUR_OF_DAY)
        val hour = if (hourOfDay == 0) 24 else hourOfDay
        val minute = calendar.get(Calendar.MINUTE)

        val currentTimeMillis = (hour * HOUR_MILLISECONDS) + (minute * MINUTES_MILLISECONDS)
        val sleepTimeMillis = (sleepHour * HOUR_MILLISECONDS) + (sleepMinute * MINUTES_MILLISECONDS)
        return if (sleepTimeMillis - currentTimeMillis <= 0) 60000 else sleepTimeMillis - currentTimeMillis
    }
}

data class UrlData(
    val isBaseUrl: Boolean,
    val url: String
)