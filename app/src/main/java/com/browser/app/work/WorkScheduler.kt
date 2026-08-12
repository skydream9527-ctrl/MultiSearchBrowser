package com.browser.app.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * v1.9.0: 集中式 WorkManager 调度入口
 * - 唯一周期任务 KEEP，避免重复创建
 * - 仅在联网 + 不在低电量时执行
 * - 默认 6 小时一次（WorkManager 最小重复间隔 15 分钟）
 */
object WorkScheduler {

    private const val DEFAULT_INTERVAL_HOURS = 6L

    fun scheduleRssSync(context: Context, intervalHours: Long = DEFAULT_INTERVAL_HOURS) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val request = PeriodicWorkRequestBuilder<SyncWorker>(intervalHours, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            SyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun cancelRssSync(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(SyncWorker.WORK_NAME)
    }
}
