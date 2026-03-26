package com.browser.app

import android.app.Application
import com.browser.app.data.BrowserDatabase

class BrowserApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        BrowserDatabase.getInstance(this)
    }
}