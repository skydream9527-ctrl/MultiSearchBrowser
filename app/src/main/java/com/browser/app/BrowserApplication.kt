package com.browser.app

import android.app.Application
import com.browser.app.data.BrowserDatabase
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class BrowserApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // 触发数据库初始化，避免首次访问主线程卡顿
        BrowserDatabase.getInstance(this)
    }
}
