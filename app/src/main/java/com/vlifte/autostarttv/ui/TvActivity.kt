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
import androidx.core.view.get
import androidx.core.view.isGone
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.vlifte.autostarttv.*
import com.vlifte.autostarttv.TvWebViewClient.Companion.BLR_LOGO_HTML
import com.vlifte.autostarttv.receiver.LockTvReceiver.Companion.ACTION_CLOSE
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
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

    private var webView: WebView? = null
    private lateinit var btnTest: MaterialButton
    private lateinit var btnSendLogs: View
    private lateinit var btnSettings: MaterialButton
    private lateinit var settingsDialog: SettingsBottomSheetDialog
    private lateinit var tvWebViewClient: TvWebViewClient

    private var needLoadBaseUrl = false

//    private lateinit var lockTvReceiver: LockTvReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tv)
        Log.d("TvActivity", "TvActivity onCreate")
        LogWriter.log(this, sBody = "TvActivity: onCreate")
        bindViews()
        settingsDialog = SettingsBottomSheetDialog(this, appSettings, lifecycleScope)
        tvWebViewClient = TvWebViewClient(this)
        Log.d("TvActivity", "TvActivity onCreate observeSettingsDialog")
        observeSettingsDialog()
//        lockTvReceiver = LockTvReceiver()
//        registerReceiver(lockTvReceiver, IntentFilter(ACTION))
        initWebView()
//        settingsDialog.getAdUrlFromAppSettings()
        observeTvWebViewClient()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(null)
        intent?.let {
            if (it.getBooleanExtra(ACTION_CLOSE, false)) {
//                tvWebViewClient.isSleepLoadFinished = true
                Log.d(
                    "TvActivity: onNewIntent",
                    ""
                )
                needLoadBaseUrl = true
                tvWebViewClient.isSleepLoadFinished = true
            }
        }
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
                if (urlData.url.isNotEmpty()) {
                    loadUrl(urlData)
                }
            }.launchWhenResumed(lifecycleScope)
        }
    }

    private fun loadUrl(urlData: UrlData) {
        if (urlData.isBaseUrl) {
            webView?.loadDataWithBaseURL(null, urlData.url, "text/html", "utf-8", null)
            webView?.loadUrl(BLR_LOGO_HTML)
        } else {
            if (urlData.url.isNotEmpty()) {
                webView?.loadUrl(urlData.url)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d("TvActivity", "TvActivity onResume")
//        if (Settings.System.canWrite(this)) {
        LogWriter.log(
            this,
            "TvActivity: SCREEN_OFF_TIMEOUT set to ${
                Settings.System.getInt(
                    this.contentResolver,
                    Settings.System.SCREEN_OFF_TIMEOUT
                )
            }"
        )
        Settings.System.putInt(
            this.contentResolver,
            Settings.System.SCREEN_OFF_TIMEOUT, (24 * 3600000)
        )
//        } else {
//            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
//            intent.data = Uri.parse("package:" + this.packageName)
//            startActivity(intent)
//        }
//        observeTvWebViewClient()

//        Log.d("TvActivity", "TvActivity onResume observeSettingsDialog")
//        observeSettingsDialog()
        settingsDialog.getTimeFromAppSettings()
        settingsDialog.getAdUrlFromAppSettings(needLoadBaseUrl)
        Toast.makeText(this, "needLoadBaseUrl $needLoadBaseUrl", Toast.LENGTH_LONG).show()
//        tvWebViewClient.isSleepLoadFinished = false
        window.apply {
            addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }
        needLoadBaseUrl = false
        tvWebViewClient.isSleepLoadFinished = false
//
//        webView = findViewById(R.id.webView)
//        webView.apply {
//            this?.let {
//                webChromeClient = object : WebChromeClient() {
//                    override fun getDefaultVideoPoster(): Bitmap? {
//                        val bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
//                        val canvas = Canvas(bitmap)
//                        canvas.drawARGB(0, 0, 0, 0)
//
//                        return bitmap
//                    }
//
//                    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
//                        Log.d("onConsoleMessage", "${consoleMessage?.sourceId()}")
//                        return super.onConsoleMessage(consoleMessage)
//                    }
//                }
//                webViewClient = tvWebViewClient
//                settings.apply {
//                    javaScriptEnabled = true
//                    domStorageEnabled = true
//                }
////                settingsDialog.getAdUrlFromAppSettings()
//            }
//        }
    }

    private fun initWebView() {
        webView = findViewById(R.id.webView)
        webView.apply {
            this?.let {
                webChromeClient = object : WebChromeClient() {
                    override fun getDefaultVideoPoster(): Bitmap? {
                        val bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(bitmap)
                        canvas.drawARGB(0, 0, 0, 0)
                        return bitmap
                    }

                    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                        Log.d("onConsoleMessage", "${consoleMessage?.sourceId()}")
                        return super.onConsoleMessage(consoleMessage)
                    }
                }
                webViewClient = tvWebViewClient
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                }
//                settingsDialog.getAdUrlFromAppSettings()
            }
        }
    }

//    override fun onDestroy() {
//        if (::lockTvReceiver.isInitialized) {
//            unregisterReceiver(lockTvReceiver)
//        }
//        super.onDestroy()
//    }

    private fun bindViews() {
        btnSendLogs = findViewById(R.id.btn_send_logs)
        btnSendLogs.setOnClickListener { sentEmail() }

        //todo delete after testing
        btnTest = findViewById<MaterialButton>(R.id.btn_test)
        btnTest.setOnClickListener {
            Toast.makeText(this, "ERROR!", Toast.LENGTH_SHORT).show()
            throw RuntimeException("Test Crash")
        }

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

    private fun <T> Flow<T>.launchWhenStarted(lifecycleScope: LifecycleCoroutineScope) {
        lifecycleScope.launchWhenStarted {
            this@launchWhenStarted.collect()
        }
    }
}