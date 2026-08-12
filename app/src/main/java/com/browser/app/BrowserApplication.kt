package com.browser.app

import android.app.Application
import com.browser.app.data.BrowserDatabase
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class BrowserApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        BrowserDatabase.getInstance(this)
    }
}
