package com.browser.app.ui.webview

import android.content.Context
import android.webkit.JavascriptInterface
import com.browser.app.repository.BookmarkRepository
import com.browser.app.repository.HistoryRepository
import com.browser.app.repository.NoteRepository
import com.browser.app.repository.PasswordRepository
import com.browser.app.repository.RssRepository
import com.browser.app.repository.WindowRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject

/**
 * v2.1.0: WebView ↔ Native JS Bridge（P0-4）
 * 注入到 window.MSB 命名空间，供网页调用本地 Room 数据库能力。
 *
 * 设计：
 * - 读方法用 runBlocking 同步返回（@JavascriptInterface 不能是 suspend，JS 端调用为同步语义）
 * - 写方法用 coroutineScope.launch 异步执行，立即返回 true 表示已接收
 * - JSON 序列化用 org.json（Android 内置，免依赖）
 *
 * 安全：
 * - 所有暴露给 JS 的方法必须显式标注 @JavascriptInterface
 * - WebviewFragment.onPageStarted 中按 origin 白名单注入，非信任域不注入
 * - 协程作用域由 Fragment 注入（viewLifecycleOwner），Fragment 销毁时自动取消
 */
class WebAppInterface(
    private val context: Context,
    private val bookmarkRepository: BookmarkRepository,
    private val historyRepository: HistoryRepository,
    private val noteRepository: NoteRepository,
    private val passwordRepository: PasswordRepository,
    private val rssRepository: RssRepository,
    private val windowRepository: WindowRepository,
    private val coroutineScope: CoroutineScope,
) {

    // ============ 书签 ============

    /** 返回书签 JSON 数组字符串：MSB.getBookmarksJson() */
    @JavascriptInterface
    fun getBookmarksJson(): String = runBlocking {
        val list = bookmarkRepository.getAllBookmarks().first()
        val arr = JSONArray()
        for (b in list) {
            arr.put(JSONObject().apply {
                put("id", b.id)
                put("title", b.title)
                put("url", b.url)
                put("timestamp", b.timestamp)
                // Android 表无 folder 字段，统一输出空串保持 Web 端结构一致
                put("folder", "")
            })
        }
        arr.toString()
    }

    /** 新增书签（异步落库，立即返回 true）：MSB.addBookmark(title, url) */
    @JavascriptInterface
    fun addBookmark(title: String, url: String): Boolean {
        if (url.isBlank()) return false
        coroutineScope.launch {
            bookmarkRepository.addBookmark(title, url)
        }
        return true
    }

    /** 删除书签（异步落库，立即返回 true）：MSB.removeBookmark(url) */
    @JavascriptInterface
    fun removeBookmark(url: String): Boolean {
        if (url.isBlank()) return false
        coroutineScope.launch {
            bookmarkRepository.removeBookmark(url)
        }
        return true
    }

    // ============ 历史 ============

    /** 返回最近 100 条历史 JSON 数组字符串：MSB.getHistoryJson() */
    @JavascriptInterface
    fun getHistoryJson(): String = runBlocking {
        val list = historyRepository.getAllHistory().first().take(100)
        val arr = JSONArray()
        for (h in list) {
            arr.put(JSONObject().apply {
                put("id", h.id)
                put("title", h.title)
                put("url", h.url)
                put("timestamp", h.timestamp)
            })
        }
        arr.toString()
    }

    /** 新增历史（异步落库，立即返回 true）：MSB.addHistory(title, url) */
    @JavascriptInterface
    fun addHistory(title: String, url: String): Boolean {
        if (url.isBlank()) return false
        coroutineScope.launch {
            historyRepository.addHistory(title, url)
        }
        return true
    }

    /** 清空历史（异步落库，立即返回 true）：MSB.clearHistory() */
    @JavascriptInterface
    fun clearHistory(): Boolean {
        coroutineScope.launch {
            historyRepository.clearHistory()
        }
        return true
    }

    // ============ 笔记 ============

    /** 返回笔记 JSON 数组字符串：MSB.getNotesJson() */
    @JavascriptInterface
    fun getNotesJson(): String = runBlocking {
        val list = noteRepository.getAllNotes().first()
        val arr = JSONArray()
        for (n in list) {
            arr.put(JSONObject().apply {
                put("id", n.id)
                put("text", n.text)
                // Web 端字段为 source（对应 sourceTitle），sourceUrl 保持同名
                put("source", n.sourceTitle)
                put("sourceUrl", n.sourceUrl)
                put("timestamp", n.timestamp)
            })
        }
        arr.toString()
    }

    /** 新增笔记（异步落库，立即返回 true）：MSB.addNote(text, sourceUrl, sourceTitle) */
    @JavascriptInterface
    fun addNote(text: String, sourceUrl: String, sourceTitle: String): Boolean {
        if (text.isBlank()) return false
        coroutineScope.launch {
            noteRepository.addNote(text, sourceUrl, sourceTitle)
        }
        return true
    }

    /** 删除笔记（异步落库，立即返回 true）：MSB.deleteNote(id) */
    @JavascriptInterface
    fun deleteNote(id: Long): Boolean {
        coroutineScope.launch {
            noteRepository.deleteById(id)
        }
        return true
    }

    // ============ 统计 ============

    /** 返回各表 count 的 JSON：MSB.getStatsJson() */
    @JavascriptInterface
    fun getStatsJson(): String = runBlocking {
        val bookmarks = bookmarkRepository.getAllBookmarks().first().size
        val history = historyRepository.getAllHistory().first().size
        val notes = noteRepository.getAllNotes().first().size
        JSONObject().apply {
            put("bookmarks", bookmarks)
            put("history", history)
            put("notes", notes)
        }.toString()
    }

    // ============ RSS ============

    /** 返回 RSS 订阅源 JSON 数组字符串：MSB.getRssFeedsJson() */
    @JavascriptInterface
    fun getRssFeedsJson(): String = runBlocking {
        val list = rssRepository.getAllFeeds().first()
        val arr = JSONArray()
        for (f in list) {
            arr.put(JSONObject().apply {
                put("id", f.id)
                put("name", f.name)
                put("url", f.url)
                // Web 端字段为 addedAt，Android 表无此字段，用 lastFetched 近似映射
                put("addedAt", f.lastFetched)
            })
        }
        arr.toString()
    }

    /** 返回最近 50 条未读 RSS 条目 JSON 数组字符串：MSB.getRssItemsJson() */
    @JavascriptInterface
    fun getRssItemsJson(): String = runBlocking {
        val list = rssRepository.getAllItems().first()
            .filter { !it.isRead }
            .take(50)
        val arr = JSONArray()
        for (item in list) {
            arr.put(JSONObject().apply {
                put("id", item.id)
                put("feedId", item.feedId)
                put("guid", item.guid)
                put("title", item.title)
                put("link", item.link)
                put("description", item.description)
                put("pubDate", item.pubDate)
                put("source", item.source)
            })
        }
        arr.toString()
    }

    /** 返回 RSS 未读条目数：MSB.getUnreadCount() */
    @JavascriptInterface
    fun getUnreadCount(): Int = runBlocking {
        rssRepository.getUnreadCount().first()
    }

    /** 新增 RSS 订阅源（异步落库，立即返回 true）：MSB.addRssFeed(name, url) */
    @JavascriptInterface
    fun addRssFeed(name: String, url: String): Boolean {
        if (url.isBlank()) return false
        coroutineScope.launch {
            rssRepository.addFeed(name, url)
        }
        return true
    }

    /** 删除 RSS 订阅源（异步落库，立即返回 true）：MSB.deleteRssFeed(id) */
    @JavascriptInterface
    fun deleteRssFeed(id: Long): Boolean {
        coroutineScope.launch {
            rssRepository.deleteFeedById(id)
        }
        return true
    }

    // ============ 多窗口 ============

    /** 返回多窗口 JSON 数组字符串：MSB.getWindowsJson() */
    @JavascriptInterface
    fun getWindowsJson(): String = runBlocking {
        val list = windowRepository.getAllWindows().first()
        val arr = JSONArray()
        for (w in list) {
            arr.put(JSONObject().apply {
                put("id", w.id)
                put("title", w.title)
                put("url", w.url)
                put("timestamp", w.timestamp)
            })
        }
        arr.toString()
    }

    /** 新增多窗口（异步落库，立即返回 true）：MSB.addWindow(title, url) */
    @JavascriptInterface
    fun addWindow(title: String, url: String): Boolean {
        if (url.isBlank()) return false
        coroutineScope.launch {
            windowRepository.addWindow(title, url)
        }
        return true
    }

    /** 删除多窗口（异步落库，立即返回 true）：MSB.deleteWindow(id) */
    @JavascriptInterface
    fun deleteWindow(id: Long): Boolean {
        coroutineScope.launch {
            windowRepository.deleteWindowById(id)
        }
        return true
    }

    /** 更新多窗口（异步落库，立即返回 true）：MSB.updateWindow(id, title, url)
     *  需先取出 Entity 再 copy 更新字段，复用 windowRepository.updateWindow */
    @JavascriptInterface
    fun updateWindow(id: Long, title: String, url: String): Boolean {
        coroutineScope.launch {
            val existing = windowRepository.getWindowById(id) ?: return@launch
            windowRepository.updateWindow(existing.copy(title = title, url = url))
        }
        return true
    }

    // ============ 密码 ============

    /** 返回密码数量（不返回明文，安全考虑）：MSB.getPasswordsCount() */
    @JavascriptInterface
    fun getPasswordsCount(): Int = runBlocking {
        passwordRepository.getAllPasswords().first().size
    }

    // ============ 兼容方法 ============

    /**
     * @Deprecated 改用 addNote(text, sourceUrl, sourceTitle)
     * 保留兼容：旧版 Web 端仍通过 MSB.saveNote() 调用。
     */
    @Deprecated("改用 addNote(text, sourceUrl, sourceTitle)", ReplaceWith("addNote(text, sourceUrl, sourceTitle)"))
    @JavascriptInterface
    fun saveNote(text: String, sourceUrl: String, sourceTitle: String) {
        if (text.isBlank()) return
        coroutineScope.launch {
            noteRepository.addNote(text, sourceUrl, sourceTitle)
        }
    }

    /** 网页获取当前应用信息（用于用户脚本/扩展） */
    @JavascriptInterface
    fun getAppInfo(): String = "MultiSearchBrowser/2.1.0 Android"

    /** 网页触发分享：MSB.share(title, url) */
    @JavascriptInterface
    fun share(title: String, url: String) {
        val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_SUBJECT, title)
            putExtra(android.content.Intent.EXTRA_TEXT, "$title\n$url")
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(
            android.content.Intent.createChooser(send, "分享到").apply {
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }
}
