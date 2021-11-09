package com.vlifte.autostarttv.ui

import android.app.Activity
import android.os.Bundle
import android.webkit.WebView
import com.vlifte.autostarttv.R

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}