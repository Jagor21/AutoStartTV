package com.vlifte.autostarttv

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import com.vlifte.autostarttv.ui.TvActivity
import com.vlifte.autostarttv.utils.TimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

private const val CONNECT_MONITOR_URL = "https://vlifte.by/_tv"
private const val VLIFTE_TV = "_tv"

class TvWebViewClient(private val context: Context) : WebViewClient() {

    private val _token = MutableStateFlow("")
    val token: StateFlow<String> = _token.asStateFlow()

    var isSleepLoadFinished: Boolean = false
    private val _urlData = MutableStateFlow(UrlData(false, ""))
    val urlData: StateFlow<UrlData> = _urlData.asStateFlow()

    private var startTime = System.currentTimeMillis()

    companion object {
        const val BLR_LOGO_HTML =
            "<html><head></head><body><img style=\"width: 100%; height: auto\" src=\"logo_blr.webp\"></body></html>"
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

//        if (url == "about:blank") {
//            Settings.System.putInt(
//                context.contentResolver,
//                Settings.System.SCREEN_OFF_TIMEOUT, (10000)
//            )
//        }
        super.onPageFinished(view, url)
    }

//    override fun onReceivedError(
//        view: WebView?,
//        request: WebResourceRequest?,
//        error: WebResourceError?
//    ) {
//        if (error?.errorCode == WebViewClient.ERROR_HOST_LOOKUP) {
//            Log.d("DISCONNECT_TAG", "${error.errorCode}")
//            CoroutineScope(Dispatchers.Default).launch {
//                _urlData.value = UrlData(true, "")
//                delay(15000)
//                request?.url?.let {
//                    _urlData.value = UrlData(false, it.toString())
//                }
////                this@TvWebViewClient.onLoadResource(view, request?.url.toString())
//            }
//        }
//        Toast.makeText(context, "${error?.description}", Toast.LENGTH_SHORT).show()
//    }

    override fun onLoadResource(view: WebView?, url: String?) {
//        if(System.currentTimeMillis() - startTime >= 14400000) {
//            val intent = Intent(context, TvActivity::class.java)
//            Log.d("RECREATE", "recreate!!!")
//            startTime = System.currentTimeMillis()
//            this.onLoadResource(view, BLR_LOGO_HTML)
//            context.startActivity(intent)
//            return
//        }
        Log.d(
            "WebView",
            "onLoadResource: your current url when webpage loading.. $url"
        )

        if (isSleepLoadFinished) {
            Log.d(
                "WebView",
                "onLoadResource: Stop: Loading placeholder"
            )

//            setUrlData(UrlData(true, BLR_LOGO_HTML))
//            Settings.System.putInt(
//                context.contentResolver,
//                Settings.System.SCREEN_OFF_TIMEOUT, (10000)
//            )
        }

        super.onLoadResource(view, url)
    }
}