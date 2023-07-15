package com.vlifte.autostarttv.ui

import android.content.*
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.webkit.*
import android.webkit.WebSettings.LOAD_CACHE_ELSE_NETWORK
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.isGone
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Player.Listener
import com.google.android.exoplayer2.database.StandaloneDatabaseProvider
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.upstream.FileDataSource
import com.google.android.exoplayer2.upstream.cache.Cache
import com.google.android.exoplayer2.upstream.cache.CacheDataSink
import com.google.android.exoplayer2.upstream.cache.CacheDataSource
import com.google.android.exoplayer2.upstream.cache.NoOpCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import com.google.android.material.button.MaterialButton
import com.sgvdev.autostart.models.AdRequest
import com.sgvdev.autostart.models.ContentX
import com.vlifte.autostarttv.*
import com.vlifte.autostarttv.TvWebViewClient.Companion.BLR_LOGO_HTML
import com.vlifte.autostarttv.receiver.LockTvReceiver
import com.vlifte.autostarttv.ui.viewmodel.MainActivityViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject


private const val DEV_EMAIL = "goshark2006@gmail.com"

@AndroidEntryPoint
class TvActivity : AppCompatActivity() {

    @Inject
    lateinit var appSettings: AppSettings

    private var webView: WebView? = null
    private lateinit var btnTest: MaterialButton
    private lateinit var btnSendLogs: View
    private lateinit var vBlackScreen: View
    private lateinit var btnSettings: MaterialButton
    private lateinit var settingsDialog: SettingsBottomSheetDialog
    private lateinit var tvWebViewClient: TvWebViewClient

    private var needLoadBaseUrl = false

    private val startTime = System.currentTimeMillis()

    private var needLoadAd = true

    private val adLinks = mutableListOf<String>()

    private val viewModel by viewModels<MainActivityViewModel>()
    private var currentCheckSum = ""
    private var currentAdList = mutableListOf<ContentX>()
    private var videoAdHashMap = hashMapOf<Int, String>()
    private lateinit var exoPlayer: ExoPlayer
    private lateinit var playerView: StyledPlayerView
    private lateinit var imageAd: ImageView

    private var downloadCache: Cache? = null
    private val DOWNLOAD_CONTENT_DIRECTORY = "downloads"

    private var duration = 10000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tv)
        Log.d("TvActivity", "TvActivity onCreate")
        LogWriter.log(this, sBody = "TvActivity: onCreate")
        bindViews()
        initExoPlayer()
        settingsDialog = SettingsBottomSheetDialog(this, appSettings, lifecycleScope)
        tvWebViewClient = TvWebViewClient(this)
        Log.d("TvActivity", "TvActivity onCreate observeSettingsDialog")

        observeViewModel()
        observeLockReceiver()
        //UNCOMMENT LATER


//        lockTvReceiver = LockTvReceiver()
//        registerReceiver(lockTvReceiver, IntentFilter(ACTION))

        //UNCOMMENT LATER
        initWebView()

//        settingsDialog.getAdUrlFromAppSettings()

        //UNCOMMENT LATER
        observeTvWebViewClient()
//        observeSettingsDialog()

        viewModel.needLoadAd = true
    }

    private fun observeViewModel() {
        viewModel.ad.observe(this) {
            Log.d("AD_RESPONSE", it.toString())
            it.success.checksum?.let { newChecksum ->
                if (currentCheckSum != newChecksum) {
                    currentCheckSum = newChecksum
                    currentAdList.clear()
                    it.success.content?.let {
                        it.advertisement?.let {
                            currentAdList.addAll(it.content)
                            videoAdHashMap.clear()
                        }
                    }
                    playAd()
                }
            }
        }
    }

    private fun initExoPlayer() {
//        exoPlayer = ExoPlayer.Builder(this@TvActivity).build()
//        playerView.player = exoPlayer
//        playerView.player?.volume = 1f
//        exoPlayer.repeatMode = Player.REPEAT_MODE_OFF
    }

    private fun playAd() {
        launch {
            while (needLoadAd) {
                currentAdList.forEach { contentX ->
                    Log.d("exoPlayer", contentX.file.url)
                    webView?.isGone = true
                    if (contentX.file.url.contains(".jpg") || contentX.file.url.contains(".jpeg")) {
                        imageAd.visibility = View.VISIBLE
                        playerView.visibility = View.INVISIBLE
                        Glide.with(imageAd).load(contentX.file.url).into(imageAd)
//                        isCurrentAdVideo = false
                    } else {
                        imageAd.visibility = View.INVISIBLE
                        playerView.visibility = View.VISIBLE
                        exoPlayer = ExoPlayer.Builder(this@TvActivity).build()
                        playerView.player = exoPlayer
                        playerView.player?.volume = 1f
                        withContext(Dispatchers.Main) {
                            exoPlayer.clearMediaItems()
                            val mediaItem: MediaItem =
                                MediaItem.fromUri(contentX.file.url)
//                            val mediaSource = ProgressiveMediaSource.Factory(DefaultHttpDataSource.Factory()).createMediaSource(mediaItem)
                            val mediaSource =
                                ProgressiveMediaSource.Factory(buildCacheDataSourceFactory())
                                    .createMediaSource(mediaItem)

                            exoPlayer.setMediaSource(mediaSource)
                            exoPlayer.repeatMode = Player.REPEAT_MODE_OFF
                            exoPlayer.playWhenReady = true
                            exoPlayer.prepare()
//                            Log.d("VIDEO_DURATION", "video duration is ${exoPlayer.duration / 1000}")
                            exoPlayer.addListener(object : Listener {
                                override fun onPlayerStateChanged(
                                    playWhenReady: Boolean,
                                    playbackState: Int
                                ) {
                                    if (playbackState == ExoPlayer.STATE_READY) {
                                        val realDurationMillis = exoPlayer.duration
                                        duration = realDurationMillis
                                    }
                                    if (playbackState == ExoPlayer.STATE_ENDED) {
                                    }
                                }
                            })
                        }
//                        isCurrentAdVideo = true
                    }
// TODO: UNCOMMENT THIS
//                    delay(/*if (isCurrentAdVideo) exoPlayer.duration + 3000L else*/ contentX.duration.toInt() * 1000L)
                    Log.d("VIDEO_DURATION", "video duration is $duration")
                    Log.d("VIDEO_DURATION", "contentX video duration is ${contentX.duration.toInt() * 1000L}")
                    delay(duration)


//                    exoPlayer.clearMediaItems()

                    try {
                        exoPlayer.release()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
//                currentAdList.forEach {
//                    Log.d("PLAY_AD", it.file.url)
////                    webView?.loadData(it.file.url, "text/html", "utf-8")
//                    webView?.loadUrl(it.file.url)
//                    delay(it.duration.toInt() * 1000L)
//                }
            }
        }
    }

    @Synchronized
    fun buildCacheDataSourceFactory(): DataSource.Factory {
        val cache = getDownloadCache()
        val cacheSink = CacheDataSink.Factory()
            .setCache(cache)
        val upstreamFactory = DefaultDataSource.Factory(this, DefaultHttpDataSource.Factory())
        return CacheDataSource.Factory()
            .setCache(cache)
            .setCacheWriteDataSinkFactory(cacheSink)
            .setCacheReadDataSourceFactory(FileDataSource.Factory())
            .setUpstreamDataSourceFactory(upstreamFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    @Synchronized
    private fun getDownloadCache(): Cache {
        if (downloadCache == null) {
            val downloadContentDirectory = File(
                getExternalFilesDir(null),
                DOWNLOAD_CONTENT_DIRECTORY
            )
            downloadCache =
                SimpleCache(
                    downloadContentDirectory,
                    NoOpCacheEvictor(),
                    StandaloneDatabaseProvider(this)
                )
        }
        return downloadCache!!
    }

    private fun observeLockReceiver() {
        LockTvReceiver.event.onEach { event ->
            when (event) {
                LockScreenCodeEvent.EVENT_CLOSE -> {
//                    Log.d(
//                        "TvActivity: onNewIntent",
//                        ""
//                    )
//                    webView?.loadDataWithBaseURL(
//                        "file:///android_asset/",
//                        BLR_LOGO_HTML,
//                        "text/html",
//                        "UTF-8",
//                        null
//                    )
//                    needLoadBaseUrl = true
//                    viewModel.needLoadAd = false
//                    needLoadAd = false
//                    tvWebViewClient.isSleepLoadFinished = true
                    Settings.System.putInt(
                        this@TvActivity.contentResolver,
                        Settings.System.SCREEN_OFF_TIMEOUT, (5000)
                    )
                    LockTvReceiver.resetMyEvent(LockScreenCodeEvent.NONE)
                }

                LockScreenCodeEvent.EVENT_BLACK_SCREEN -> {
                    viewModel.needLoadAd = false
                    exoPlayer.stop()
                    exoPlayer.release()
                    vBlackScreen.isGone = false
                }

                LockScreenCodeEvent.EVENT_BLACK_SCREEN_OFF -> {
                    downloadCache?.release()
                    downloadCache = null
                    LockTvReceiver.resetMyEvent(LockScreenCodeEvent.NONE)
                    vBlackScreen.isGone = true
                    exoPlayer.release()
                    recreate()
                }

                LockScreenCodeEvent.NONE -> {}
            }
        }.launchIn(lifecycleScope)

    }

//    override fun onNewIntent(intent: Intent?) {
//        super.onNewIntent(null)
//        intent?.let {
//            if (it.getBooleanExtra(ACTION_CLOSE, false)) {
////                tvWebViewClient.isSleepLoadFinished = true
//                Log.d(
//                    "TvActivity: onNewIntent",
//                    ""
//                )
//                webView?.loadDataWithBaseURL(
//                    "file:///android_asset/",
//                    BLR_LOGO_HTML,
//                    "text/html",
//                    "UTF-8",
//                    null
//                )
//                needLoadBaseUrl = true
//                viewModel.needLoadAd = false
//                needLoadAd = false
//                tvWebViewClient.isSleepLoadFinished = true
//                Settings.System.putInt(
//                    this.contentResolver,
//                    Settings.System.SCREEN_OFF_TIMEOUT, (5000)
//                )
//            }
//
//            if (it.getBooleanExtra(ACTION_BLACK_SCREEN, false)) {
//                viewModel.needLoadAd = false
//                exoPlayer.stop()
//                vBlackScreen.isGone = false
//            } else {
//                recreate()
//            }
//        }
//    }


    private fun observeTvWebViewClient() {
        tvWebViewClient.apply {
            token.onEach { token ->
                if (token.isNotEmpty()) {
                    launch {
                        appSettings.saved.collect {
                            val oldToken = it.deviceToken
                            if (oldToken != token) {
                                appSettings.setDeviceToken(token)
                            }
                        }
                        Log.d(
                            "TvActivity: WebView",
                            "onPageFinished: your current token = $token"
                        )
                        LogWriter.log(
                            this@TvActivity,
                            "TvActivity: WebView onPageFinished: your current token = $token"
                        )
                        webView?.loadUrl("")
                        viewModel.getAd(AdRequest(token))
                    }
                }
            }.launchWhenResumed(lifecycleScope)

            urlData.onEach { urlData ->
                if (urlData.url.isNotEmpty()) {
                    loadUrl(urlData)
                }
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
//            webView?.loadDataWithBaseURL(null, urlData.url, "text/html", "utf-8", null)
            webView?.loadDataWithBaseURL(
                "file:///android_asset/",
                BLR_LOGO_HTML,
                "text/html",
                "UTF-8",
                null
            )
//            webView?.loadUrl(BLR_LOGO_HTML)
        } else {
            if (urlData.url.isNotEmpty()) {
                if (urlData.url.contains("data")) {
                    webView?.loadData(urlData.url, "text/html", "utf-8")
                } else {
                    webView?.loadUrl(urlData.url)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d("TvActivity", "TvActivity onResume")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.System.canWrite(this)) {
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
            } else {
                val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                intent.data = Uri.parse("package:" + this.packageName)
                startActivity(intent)
            }
        }
        observeTvWebViewClient()

//        Log.d("TvActivity", "TvActivity onResume observeSettingsDialog")
        observeSettingsDialog()
        settingsDialog.getTimeFromAppSettings()
        settingsDialog.getAdUrlFromAppSettings(needLoadBaseUrl)
        Toast.makeText(this, "needLoadBaseUrl $needLoadBaseUrl", Toast.LENGTH_LONG).show()
//        tvWebViewClient.isSleepLoadFinished = false
        window.apply {
            addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }
        needLoadBaseUrl = false
        tvWebViewClient.isSleepLoadFinished = false
        needLoadAd = true

        initWebView()
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

        checkToken()
        vBlackScreen.isGone = true
    }

    private fun checkToken() {
        launch {
            appSettings.saved.collect {
                if (it.deviceToken.isEmpty()) {
                    webView?.loadUrl("https://www.vlifte.by/_tv/")
                } else {
                    viewModel.getAd(AdRequest(it.deviceToken))
                }
            }
        }
    }

    private fun initWebView() {
        webView = findViewById(R.id.webView)
        webView.apply {
            this?.let {
                webChromeClient = object : WebChromeClient() {
                    override fun getDefaultVideoPoster(): Bitmap? {
                        return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
                    }

                    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                        Log.d("onConsoleMessage", "${consoleMessage?.sourceId()}")
                        return super.onConsoleMessage(consoleMessage)
                    }
                }
                webViewClient = tvWebViewClient
                settings.apply {
                    javaScriptEnabled = true
//                    domStorageEnabled = true
                    settings.cacheMode = LOAD_CACHE_ELSE_NETWORK
                    settings.loadsImagesAutomatically = true
                    settings.mediaPlaybackRequiresUserGesture = false
                }

//                settingsDialog.getAdUrlFromAppSettings()
            }
        }
//        val urls = listOf(
//            "https://vlifte.by/content/testPicVideo/NwMP4/n2.mp4",
//            "https://vlifte.by/content/Main_prog/Moby2.mp4",
//            "https://vlifte.by/content/testPicVideo/NwMP4/n3.mp4",
//            "https://vlifte.by/content/Main_prog/Moby.mp4",
//            "https://vlifte.by/content/Main_prog/Ararat.mp4",
//            "https://vlifte.by/content/Main_prog/Xiaomi_ATV_2.mp4",
//            "https://vlifte.by/content/Main_prog/Xiaomi_ATV_3.mp4",
//            "https://vlifte.by/content/Main_prog/A1_Smart_Sale.mp4",
//        )
//        launch {
//            while (needLoadAd) {
//                for (url in adLinks) {
//                    webView?.loadUrl(url)
//                    delay(10000)
//                }
//            }
//        }
//        webView?.loadDataWithBaseURL("file:///android_asset/", BLR_LOGO_HTML, "text/html", "UTF-8", null)
    }

    override fun onDestroy() {
//        if (::lockTvReceiver.isInitialized) {
//            unregisterReceiver(lockTvReceiver)
//        }
        exoPlayer.release()
        super.onDestroy()
    }

    private fun bindViews() {
        btnSendLogs = findViewById(R.id.btn_send_logs)
        btnSendLogs.setOnClickListener { sentEmail() }
        btnSettings = findViewById(R.id.btn_settings)
        btnSettings.setOnClickListener {
            settingsDialog.open()
        }
        playerView = findViewById(R.id.player_view)
        playerView.hideController()
        playerView.controllerAutoShow = false
        imageAd = findViewById(R.id.iv_ad)
        exoPlayer = ExoPlayer.Builder(this).build()
        vBlackScreen = findViewById(R.id.v_black_screen)
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