package com.browser.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 应用内下载记录。
 *
 * 与系统 [android.app.DownloadManager] 的 downloadId 一一对应：
 * 启动下载时拿到 downloadId 后插入一条 [DownloadEntity]，
 * 系统 ACTION_DOWNLOAD_COMPLETE 广播到达后再回填本地路径与最终状态。
 */
@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    /** 系统 DownloadManager 返回的下载 id，用于查询状态 */
    val downloadId: Long,
    val title: String,
    val url: String,
    val mimetype: String? = null,
    /** 本地文件 Uri 字符串，下载完成后回填 */
    val localUri: String? = null,
    /** 状态码：直接存 DownloadManager.STATUS_*（PENDING=1, RUNNING=2, PAUSED=4, SUCCESSFUL=8, FAILED=16） */
    val status: Int = 1,
    val totalBytes: Long = 0,
    val downloadedBytes: Long = 0,
    val timestamp: Long = System.currentTimeMillis()
)
