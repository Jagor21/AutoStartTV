package com.vlifte.autostarttv

import android.content.*
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.io.File
import java.util.*
import javax.inject.Inject

private const val WRITE_SETTINGS_PERMISSION_CODE = 111
private const val RESULT_ENABLE_ADMIN_REQUEST_CODE = 222

private const val HOUR_MILLISECONDS = 3600000
private const val MINUTES_MILLISECONDS = 60000

private const val SLEEP_HOUR = 22
private const val WAKE_UP_HOUR = 7
private const val SLEEP_MINUTE = 60

private const val CONNECT_MONITOR_URL = "https://vlifte.by/_tv"
private const val AD_URL = "https://vlifte.by/"

@AndroidEntryPoint
class TvActivity : AppCompatActivity() {

    @Inject
    lateinit var appSettings: AppSettings

    private lateinit var webView: WebView
    private lateinit var btnSendLogs: View
    private lateinit var parent: RelativeLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_tv)
        Log.d("MY_TAG", "TvActivity onCreate")

        LogWriter.log(this, sBody = "TvActivity: onCreate")

        bindViews()
    }

    override fun onResume() {
        super.onResume()
        webView = findViewById(R.id.webView)
        parent = findViewById(R.id.parent)

        webView.apply {
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String) {
                    Log.d(
                        "WebView",
                        "onPageFinished: your current url when webpage loading.. finish $url"
                    )

                    val token = url.substringAfter(CONNECT_MONITOR_URL).replace("/", "")
                    if (token.isNotEmpty() && !token.contains("vlifte") && token != "about:blank") {
                        lifecycleScope.launch {
                            appSettings.setDeviceToken(token)
                            Log.d(
                                "WebView",
                                "onPageFinished: your current token = $token"
                            )
                        }
                    }
                    super.onPageFinished(view, url)
                }

                override fun onLoadResource(view: WebView?, url: String?) {
                    val currentTime = getCurrentTimeMillis()
                    if (currentTime.hourDiffMillis == 0 && currentTime.minuteDiffMillis == 0 && currentTime.seconds <= 30) {
                        webView.loadUrl("")
                        Log.d(
                            "WebView",
                            "onLoadResource: stop"
                        )
                    }
                    Log.d(
                        "WebView",
                        "onLoadResource: your current url when webpage loading.. $url"
                    )
                    super.onLoadResource(view, url)

                }
            }

            lifecycleScope.launch {
                appSettings.saved.collect {
                    if (it.deviceToken.isEmpty()) {
                        loadUrl(CONNECT_MONITOR_URL)
                    } else {
                        loadUrl("$CONNECT_MONITOR_URL/${it.deviceToken}")
                    }
                }
            }

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
            }


            window.apply {
                addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            }
        }
//        val calendar = Calendar.getInstance()
//        val hour = calendar.get(Calendar.HOUR_OF_DAY)
//        val minute = calendar.get(Calendar.MINUTE)
//        val hourDiff = if (hour in WAKE_UP_HOUR..SLEEP_HOUR) (SLEEP_HOUR - hour) else 0
//        val hourDiffMillis = hourDiff * HOUR_MILLISECONDS
//        val minuteDiffMillis =
//            if (hour in WAKE_UP_HOUR..SLEEP_HOUR) (SLEEP_MINUTE - minute) * MINUTES_MILLISECONDS else /*0*/ 60000
        val currentTime = getCurrentTimeMillis()
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
                Settings.System.SCREEN_OFF_TIMEOUT, (currentTime.getFullTimeMillis())
            )

            LogWriter.log(
                this,
                "\nMainActivity: onResume() defaultScreenTimeOut = $defaultScreenTimeOut timeSetSuccessfully = $timeSetSuccessfully\nSystem will go sleep in ${if (currentTime.hourDiff != 0) " $currentTime.hourDiff hours" else ""} ${currentTime.minuteDiffMillis / MINUTES_MILLISECONDS} minutes\n"
            )
            Log.d(
                "MY_TAG",
                "\nMainActivity: onResume() defaultScreenTimeOut = $defaultScreenTimeOut timeSetSuccessfully = $timeSetSuccessfully\nSystem will go sleep in${if (currentTime.hourDiff != 0) " ${currentTime.hourDiff} hours" else ""} ${currentTime.minuteDiffMillis / MINUTES_MILLISECONDS} minutes\n"
            )
        }
    }

    private fun getCurrentTimeMillis(): CurrentTime {

        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val seconds = calendar.get(Calendar.SECOND)

        val hourDiff = if (hour in WAKE_UP_HOUR..SLEEP_HOUR) (SLEEP_HOUR - hour) else 0
        val hourDiffMillis = hourDiff * HOUR_MILLISECONDS
        val minuteDiffMillis =
            if (hour in WAKE_UP_HOUR..SLEEP_HOUR) (SLEEP_MINUTE - minute) * MINUTES_MILLISECONDS else 0

        return CurrentTime(
            hourDiffMillis = hourDiffMillis,
            minuteDiffMillis = minuteDiffMillis,
            hourDiff = hourDiff,
            seconds = seconds
        )
    }


    private fun bindViews() {
        btnSendLogs = findViewById(R.id.btn_send_logs)
        btnSendLogs.setOnClickListener { sentEmail() }
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
}

data class CurrentTime(
    val hourDiffMillis: Int,
    val minuteDiffMillis: Int,
    val hourDiff: Int,
    val seconds: Int
) {
    fun getFullTimeMillis() = hourDiffMillis + minuteDiffMillis
}