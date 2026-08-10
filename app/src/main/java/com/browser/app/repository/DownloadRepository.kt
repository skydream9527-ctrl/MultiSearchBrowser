package com.browser.app.repository

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.browser.app.data.dao.DownloadDao
import com.browser.app.data.entity.DownloadEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 下载记录仓库：包装 DAO + 系统 DownloadManager。
 *
 * - 启动下载时由调用方（WebviewFragment）自行 enqueue 拿到 downloadId，
 *   再调用 [insertRecord] 落库一条 [DownloadEntity]。
 * - 收到 ACTION_DOWNLOAD_COMPLETE 时调用 [refreshStatusByDownloadId]，
 *   查询 DownloadManager 拿最终状态回填到 DB。
 */
@Singleton
class DownloadRepository @Inject constructor(
    private val downloadDao: DownloadDao,
    @ApplicationContext private val context: Context
) {
    fun getAllDownloads(): Flow<List<DownloadEntity>> = downloadDao.getAllDownloads()

    /**
     * 插入一条下载记录。WebviewFragment.handleDownload 调用 enqueue 拿到 downloadId 后调用此方法。
     */
    suspend fun insertRecord(
        downloadId: Long,
        title: String,
        url: String,
        mimetype: String?
    ): Long {
        return downloadDao.insert(
            DownloadEntity(
                downloadId = downloadId,
                title = title,
                url = url,
                mimetype = mimetype,
                status = DownloadManager.STATUS_PENDING
            )
        )
    }

    /**
     * 系统 ACTION_DOWNLOAD_COMPLETE 到达后调用：查 DownloadManager 拿最终状态，
     * 把对应记录的 status / localUri / bytes 同步到 DB。
     */
    suspend fun refreshStatusByDownloadId(downloadId: Long) {
        val entity = downloadDao.getByDownloadId(downloadId) ?: return
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val cursor = dm.query(DownloadManager.Query().setFilterById(downloadId)) ?: return
        cursor.use {
            if (!it.moveToFirst()) return
            val status = it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            val total = it.getLong(it.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
            val downloaded =
                it.getLong(it.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
            val localUri = if (status == DownloadManager.STATUS_SUCCESSFUL) {
                it.getString(it.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI))
            } else {
                null
            }
            downloadDao.update(
                entity.copy(
                    status = status,
                    totalBytes = total,
                    downloadedBytes = downloaded,
                    localUri = localUri ?: entity.localUri
                )
            )
        }
    }

    suspend fun deleteRecord(id: Long) {
        downloadDao.deleteById(id)
    }

    /** 打开下载完成的文件：通过 ContentResolver + Intent.ACTION_VIEW */
    fun buildOpenIntent(entity: DownloadEntity): Intent? {
        val uri = entity.localUri?.let { Uri.parse(it) } ?: return null
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, entity.mimetype ?: "*/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
