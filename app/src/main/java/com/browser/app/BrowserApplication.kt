package com.browser.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.browser.app.data.BrowserDatabase
import com.browser.app.work.WorkScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * v1.9.0: 实现 Configuration.Provider，让 WorkManager 使用 HiltWorkerFactory
 * 注入 @HiltWorker 标注的 Worker 类。
 */
@HiltAndroidApp
class BrowserApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        BrowserDatabase.getInstance(this)
        // 调度 RSS 周期同步（唯一任务，重复启动不会创建新任务）
        WorkScheduler.scheduleRssSync(this)
    }
}
