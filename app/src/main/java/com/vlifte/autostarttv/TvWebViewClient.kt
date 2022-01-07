package com.vlifte.autostarttv

import android.content.Context
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import com.vlifte.autostarttv.utils.TimeUtils
import kotlinx.coroutines.flow.*

private const val CONNECT_MONITOR_URL = "https://vlifte.by/_tv"
private const val VLIFTE_TV = "_tv"

class TvWebViewClient(private val context: Context) : WebViewClient() {

    private val _token = MutableStateFlow("")
    val token: StateFlow<String> = _token.asStateFlow()

    var isSleepLoadFinished: Boolean = false
    private val _urlData = MutableStateFlow(UrlData(false, ""))
    val urlData: StateFlow<UrlData> = _urlData.asStateFlow()

    companion object {
        const val BLR_LOGO_HTML =
            "<html><head></head><body><img style=\"width: 100%; height: auto\" src=\"file:///android_asset/logo_blr.webp\"></body></html>"
    }

    override fun onPageFinished(view: WebView?, url: String) {
        Log.d(
            "WebView",
            "onPageFinished: your current url when webpage loading.. finish $url"
        )

        if (url.contains(VLIFTE_TV)) {
            val token = url.substringAfter(CONNECT_MONITOR_URL).replace("/", "")
            if (token.isNotEmpty()) {
                _token.value = token
            }
        }
        super.onPageFinished(view, url)
    }

    override fun onLoadResource(view: WebView?, url: String?) {
//        val currentTime = TimeUtils.getCurrentTimeMillis()
//        if (currentTime == 60000 && !isSleepLoadFinished) {
//            setUrlData(UrlData(true, BLR_LOGO_HTML))
//            Log.d("WebView", "onLoadResource: stop")
//            LogWriter.log(context, sBody = "TvActivity: webView onLoadResource stop")
//            isSleepLoadFinished = true
//        }
        Log.d(
            "WebView",
            "onLoadResource: your current url when webpage loading.. $url"
        )
        super.onLoadResource(view, url)
    }

    private fun setUrlData(urlData: UrlData) {
        _urlData.value = UrlData(false, "")
        _urlData.value = urlData
    }
}