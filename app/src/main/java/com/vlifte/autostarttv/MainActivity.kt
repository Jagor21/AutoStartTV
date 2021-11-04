package com.vlifte.autostarttv

import android.app.Activity
import android.os.Bundle
import android.webkit.WebView

class MainActivity : Activity() {

private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        webView = findViewById(R.id.webView)

//        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}