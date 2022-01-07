package com.vlifte.autostarttv.ui

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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.vlifte.autostarttv.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.util.*
import javax.inject.Inject

private const val DEV_EMAIL = "goshark2006@gmail.com"

@AndroidEntryPoint
class TvActivity : AppCompatActivity() {

    @Inject
    lateinit var appSettings: AppSettings

    private lateinit var webView: WebView
    private lateinit var btnSendLogs: View
    private lateinit var btnSettings: MaterialButton

    private lateinit var settingsDialog: SettingsBottomSheetDialog
    private lateinit var tvWebViewClient: TvWebViewClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tv)
        Log.d("TvActivity", "TvActivity onCreate")
        LogWriter.log(this, sBody = "TvActivity: onCreate")
        bindViews()
        settingsDialog = SettingsBottomSheetDialog(this, appSettings, lifecycleScope)
        tvWebViewClient = TvWebViewClient(this)
    }

    private fun observeTvWebViewClient() {
        tvWebViewClient.apply {
            token.onEach { token ->
                launch {
                    appSettings.setDeviceToken(token)
                    Log.d(
                        "TvActivity: WebView",
                        "onPageFinished: your current token = $token"
                    )
                    LogWriter.log(
                        this@TvActivity,
                        "TvActivity: WebView onPageFinished: your current token = $token"
                    )
                }
            }.launchWhenResumed(lifecycleScope)

            urlData.onEach { urlData ->
                loadUrl(urlData)
            }.launchWhenResumed(lifecycleScope)
        }
    }

    private fun observeSettingsDialog() {
        settingsDialog.apply {
            webViewUrl.onEach { urlData ->
                loadUrl(urlData)
            }.launchWhenResumed(lifecycleScope)
        }
    }

    private fun loadUrl(urlData: UrlData) {
        if (urlData.isBaseUrl) {
            webView.loadDataWithBaseURL(null, urlData.url, "text/html", "utf-8", null)
        } else {
            if (urlData.url.isNotEmpty()) {
                webView.loadUrl(urlData.url)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Settings.System.putInt(
            this.contentResolver,
            Settings.System.SCREEN_OFF_TIMEOUT, (24 * 3600000)
        )
        observeTvWebViewClient()
        observeSettingsDialog()
        settingsDialog.getTimeFromAppSettings()
        tvWebViewClient.isSleepLoadFinished = false
        window.apply {
            addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }

        webView = findViewById(R.id.webView)
        webView.apply {
            webChromeClient = object : WebChromeClient() {
                override fun getDefaultVideoPoster(): Bitmap? {
                    val bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    canvas.drawARGB(0, 0, 0, 0)

                    return bitmap
                }
            }
            webViewClient = tvWebViewClient
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
            }
            settingsDialog.getAdUrlFromAppSettings()
        }
    }

    private fun bindViews() {
        btnSendLogs = findViewById(R.id.btn_send_logs)
        btnSendLogs.setOnClickListener { sentEmail() }

        btnSettings = findViewById(R.id.btn_settings)
        btnSettings.setOnClickListener {
            settingsDialog.open()
        }
    }

    private fun sentEmail() {
        val addresses = arrayOf(DEV_EMAIL)
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

    private fun <T> Flow<T>.launchWhenResumed(lifecycleScope: LifecycleCoroutineScope) {
        lifecycleScope.launchWhenResumed {
            this@launchWhenResumed.collect()
        }
    }
}