package com.browser.app.utils

import android.webkit.WebView
import android.webkit.WebViewClient

class BrowserWebViewClient : WebViewClient() {
    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
        url?.let {
            view?.loadUrl(it)
        }
        return true
    }
}