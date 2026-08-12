package com.browser.app.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.browser.app.data.entity.RssItemEntity
import com.browser.app.repository.RssRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * v1.9.0: 后台定时同步 Worker
 * - 拉取启用的 RSS Feed，解析最新条目入库
 * - 清理 30 天前的旧 Item，控制数据库体积
 * - 失败自动重试，遵循 WorkManager 退避策略
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val rssRepository: RssRepository,
) : CoroutineWorker(appContext, params) {

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    override suspend fun doWork(): Result {
        return try {
            val feeds = rssRepository.getEnabledFeeds().first()
            var totalNew = 0
            for (feed in feeds) {
                totalNew += fetchFeed(feed.id, feed.url)
                rssRepository.updateLastFetched(feed.id, System.currentTimeMillis())
            }
            // 清理 30 天前的旧 Item
            val cutoff = System.currentTimeMillis() - 30L * 24 * 3600 * 1000
            rssRepository.deleteOldItems(cutoff)
            Result.success()
        } catch (e: Exception) {
            // 失败重试，最多 3 次（WorkManager 默认约束）
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    /**
     * 拉取并解析一个 RSS 源，返回新增条目数。
     * 支持 RSS 2.0 与 Atom <entry> 结构的最小化解析。
     */
    private suspend fun fetchFeed(feedId: Long, url: String): Int {
        val request = Request.Builder().url(url).build()
        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) return 0
        val body = response.body?.byteStream() ?: return 0

        val parser = XmlPullParserFactory.newInstance().newPullParser()
        parser.setInput(body, null)
        var event = parser.eventType
        var newCount = 0
        var currentTag = ""
        var title = ""
        var link = ""
        var description = ""
        var pubDate = System.currentTimeMillis()
        var guid = ""

        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    currentTag = parser.name
                    if (parser.name == "item" || parser.name == "entry") {
                        title = ""; link = ""; description = ""; pubDate = System.currentTimeMillis(); guid = ""
                    }
                }
                XmlPullParser.TEXT -> {
                    val text = parser.text?.trim().orEmpty()
                    when (currentTag) {
                        "title" -> if (text.isNotEmpty()) title = text
                        "link" -> if (text.isNotEmpty() && link.isEmpty()) link = text
                        "description", "summary" -> if (text.isNotEmpty()) description = text
                        "guid", "id" -> if (text.isNotEmpty()) guid = text
                        "pubDate", "published", "updated" -> pubDate = parseDate(text)
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "item" || parser.name == "entry") {
                        if (guid.isEmpty()) guid = link.ifEmpty { title }
                        if (title.isNotEmpty() || link.isNotEmpty()) {
                            rssRepository.insertItem(
                                RssItemEntity(
                                    feedId = feedId,
                                    guid = guid,
                                    title = title,
                                    link = link,
                                    description = description,
                                    pubDate = pubDate,
                                    isRead = false,
                                    source = ""
                                )
                            )
                            newCount++
                        }
                    }
                    currentTag = ""
                }
            }
            event = parser.next()
        }
        return newCount
    }

    private fun parseDate(text: String): Long {
        if (text.isEmpty()) return System.currentTimeMillis()
        val patterns = listOf(
            "EEE, dd MMM yyyy HH:mm:ss zzz",
            "EEE, dd MMM yyyy HH:mm:ss Z",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd HH:mm:ss"
        )
        for (p in patterns) {
            try {
                return SimpleDateFormat(p, Locale.US).parse(text)?.time ?: continue
            } catch (_: Exception) { /* try next */ }
        }
        return System.currentTimeMillis()
    }

    companion object {
        const val WORK_NAME = "msb_rss_sync"
    }
}
