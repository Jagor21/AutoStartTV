package com.vlifte.autostarttv

import android.content.*
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.webkit.*
import android.widget.*
import androidx.annotation.ArrayRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.util.*
import javax.inject.Inject

private const val HOUR_MILLISECONDS = 3600000
private const val MINUTES_MILLISECONDS = 60000

private const val DEFAULT_SLEEP_HOUR = 23

private const val CONNECT_MONITOR_URL = "https://vlifte.by/_tv"
private const val VLIFTE = "vlifte"
private const val VLIFTE_TV = "_tv"

@AndroidEntryPoint
class TvActivity : AppCompatActivity() {

    @Inject
    lateinit var appSettings: AppSettings

    private lateinit var webView: WebView
    private lateinit var btnSendLogs: View
    private lateinit var btnSettings: MaterialButton

    private var sleepHour: Int = 0
    private var sleepMinute: Int = 0

    private var adUrl: String = ""
    private var isSleepLoadFinished: Boolean = false

    private lateinit var settingsDialog: SettingsBottomSheetDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tv)
        Log.d("MY_TAG", "TvActivity onCreate")
        LogWriter.log(this, sBody = "TvActivity: onCreate")
        bindViews()
        settingsDialog = SettingsBottomSheetDialog(this, lifecycleScope)
        settingsDialog.webViewUrl.onEach { urlData ->
            if(urlData.isBaseUrl) {
                webView.loadDataWithBaseURL(null, urlData.url, "text/html", "utf-8", null)
            } else {
                if(urlData.url.isNotEmpty()) {
                    webView.loadUrl(urlData.url)
                }
            }
        }.launchWhenStarted(lifecycleScope)
    }

    override fun onResume() {
        super.onResume()
        getTimeFromAppSettings()
        isSleepLoadFinished = false
        window.apply {
            addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }

        webView = findViewById(R.id.webView)
        webView.apply {
            webChromeClient = object: WebChromeClient() {
                override fun getDefaultVideoPoster(): Bitmap? {
                    val bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    canvas.drawARGB(0, 0, 0, 0)

                    return bitmap
                }
            }
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String) {
                    Log.d(
                        "WebView",
                        "onPageFinished: your current url when webpage loading.. finish $url"
                    )

                    if (url.contains(VLIFTE_TV)) {
                        val token = url.substringAfter(CONNECT_MONITOR_URL).replace("/", "")
                        if (token.isNotEmpty()) {
                            lifecycleScope.launch {
                                appSettings.setDeviceToken(token)
                                Log.d(
                                    "WebView",
                                    "onPageFinished: your current token = $token"
                                )
                            }
                        }
                    }
                    super.onPageFinished(view, url)
                }

                override fun onLoadResource(view: WebView?, url: String?) {
                    val currentTime = getCurrentTimeMillis()
                    if (currentTime == 60000 && !isSleepLoadFinished) {
                        val sHtmlTemplate =
                            "<html><head></head><body><img style=\"width: 100%; height: auto\" src=\"file:///android_asset/white_noise_gif.gif\"></body></html>"
                        webView.loadDataWithBaseURL(null, sHtmlTemplate, "text/html", "utf-8", null)
                        Log.d("WebView", "onPageFinished: stop")
                        LogWriter.log(this@TvActivity, sBody = "TvActivity: webView onLoading stop")
                        isSleepLoadFinished = true
                    }
                    Log.d(
                        "WebView",
                        "onLoadResource: your current url when webpage loading.. $url"
                    )
                    super.onLoadResource(view, url)
                }
            }
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
            }
            getAdUrlFromAppSettings()
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

    private fun getAdUrlFromAppSettings() {
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
                webView.loadUrl(adUrl)
            }
        }
    }

    private fun setScreenTimeOut(currentTimeMillis: Int) {
        val settingsCanWrite = Settings.System.canWrite(this)
        if (!settingsCanWrite) {
            Toast.makeText(
                this,
                "Разрешите изменения системных настроек!\nНастройки -> Приложения -> Специальный доступ -> Изменение системных настроек",
                Toast.LENGTH_LONG
            ).show()
        } else {
            val defaultScreenTimeOut =
                Settings.System.getInt(contentResolver, Settings.System.SCREEN_OFF_TIMEOUT)
            val timeSetSuccessfully = Settings.System.putInt(
                contentResolver,
                Settings.System.SCREEN_OFF_TIMEOUT, (currentTimeMillis)
            )

            LogWriter.log(
                this,
                "\nMainActivity: onResume() defaultScreenTimeOut = $defaultScreenTimeOut timeSetSuccessfully = $timeSetSuccessfully\nSystem will go sleep in ${currentTimeMillis/ HOUR_MILLISECONDS} hours ${currentTimeMillis / MINUTES_MILLISECONDS} minutes\n"
            )
            Log.d(
                "MY_TAG",
                "\nMainActivity: onResume() defaultScreenTimeOut = $defaultScreenTimeOut timeSetSuccessfully = $timeSetSuccessfully\nSystem will go sleep in ${currentTimeMillis/ HOUR_MILLISECONDS} hours ${(currentTimeMillis / MINUTES_MILLISECONDS)%60} minutes\n"
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


    private fun bindViews() {
        btnSendLogs = findViewById(R.id.btn_send_logs)
        btnSendLogs.setOnClickListener { sentEmail() }

        btnSettings = findViewById(R.id.btn_settings)
        btnSettings.setOnClickListener {
            openSettings()
        }
    }

    private fun openSettings() {
        val bottomSheet = BottomSheetDialog(this)
        bottomSheet.setContentView(R.layout.settings_bottom_sheet)

        val etUrl: EditText? = bottomSheet.findViewById(R.id.et_url)

        val spinnerSleepHour: Spinner? = bottomSheet.findViewById(R.id.spinner_sleep_hour)
        val spinnerSleepMinute: Spinner? = bottomSheet.findViewById(R.id.spinner_sleep_minute)

        val btnSetUrl: MaterialButton? = bottomSheet.findViewById(R.id.btn_set_url)
        val btnSetSleepTime: MaterialButton? = bottomSheet.findViewById(R.id.btn_set_sleep_time)
        val btnClose: ImageView? = bottomSheet.findViewById(R.id.iv_close)

        val btnVlifteUrl: MaterialButton? = bottomSheet.findViewById(R.id.btn_vlifte_url)
        val btnGoogleUrl: MaterialButton? = bottomSheet.findViewById(R.id.btn_google_url)
        val btnClearToken: MaterialButton? = bottomSheet.findViewById(R.id.btn_clear_token)
        val btnClearLogs: MaterialButton? = bottomSheet.findViewById(R.id.btn_clear_logs)
        val btnSleepImmediately: MaterialButton? = bottomSheet.findViewById(R.id.btn_sleep_immediately)

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
                LogWriter.clearLog(this)
                showToast("Log was cleared!")
            }
        }

        btnSleepImmediately?.let { btn ->
            btn.setOnClickListener {
                val sHtmlTemplate =
                    "<html><head></head><body><img style=\"width: 100%; height: auto\" src=\"file:///android_asset/white_noise_gif.gif\"></body></html>"
                webView.loadDataWithBaseURL(null, sHtmlTemplate, "text/html", "utf-8", null)
                getTimeFromAppSettings(true)
                bottomSheet.dismiss()
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
                                this@TvActivity.adUrl = newAdUrl
                                appSettings.setAdUrl(newAdUrl)
                                webView.loadUrl(newAdUrl)
                            }
                            return@launch
                        }
                        this@TvActivity.adUrl = newAdUrl
                        appSettings.setAdUrl(newAdUrl)
                        webView.loadUrl(newAdUrl)
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
                bottomSheet.dismiss()
            }
        }
        bottomSheet.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        bottomSheet.show()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun createSpinnerAdapter(
        spinner: Spinner?,
        @ArrayRes stringArrayRes: Int,
        itemSelection: Int
    ) {
        ArrayAdapter.createFromResource(
            this,
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

    private fun sentEmail() {
        val addresses = arrayOf("goshark2006@gmail.com")
        val i = Intent(Intent.ACTION_SEND).apply {
            type = "*/*"
            intent.data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, addresses)
            putExtra(Intent.EXTRA_SUBJECT, "Log files")
            val file = File("${this@TvActivity.filesDir}/${LogWriter.LOG_FILE_NAME}")
            val uri = FileProvider.getUriForFile(
                this@TvActivity,
                this@TvActivity.applicationContext.packageName + ".provider",
                file
            )
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        LogWriter.log(this, "TvActivity: sending logs via email")
        startActivity(Intent.createChooser(i, "Send Email Using: "))
    }

    private fun launch(block: suspend () -> Unit) {
        lifecycleScope.launch {
            block()
        }
    }

    fun <T> Flow<T>.launchWhenStarted(lifecycleScope: LifecycleCoroutineScope) {
        lifecycleScope.launchWhenStarted {
            this@launchWhenStarted.collect()
        }
    }
}