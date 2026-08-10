package com.browser.app

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.core.content.ContextCompat
import com.browser.app.repository.DownloadRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class BrowserApplication : android.app.Application() {

    @Inject
    lateinit var downloadRepository: DownloadRepository

    /**
     * 应用级协程作用域，专门处理不需要绑定生命周期的后台任务
     * （下载完成回写 DB 状态等）。SupervisorJob 保证单个子任务失败不会影响其它任务。
     */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val downloadReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != DownloadManager.ACTION_DOWNLOAD_COMPLETE) return
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
            if (id <= 0) return
            appScope.launch {
                downloadRepository.refreshStatusByDownloadId(id)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        // 监听系统下载完成广播：ACTION_DOWNLOAD_COMPLETE 是 protected broadcast，
        // 只能由系统发出，所以用 RECEIVER_NOT_EXPORTED 即可（Android 14+ 强制要求 flag）
        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.registerReceiver(
                this,
                downloadReceiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
        } else {
            registerReceiver(downloadReceiver, filter)
        }
    }

    override fun onTerminate() {
        unregisterReceiver(downloadReceiver)
        super.onTerminate()
    }
}
