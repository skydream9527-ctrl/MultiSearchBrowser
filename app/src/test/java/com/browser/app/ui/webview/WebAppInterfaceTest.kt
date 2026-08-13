package com.browser.app.ui.webview

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.browser.app.data.BrowserDatabase
import com.browser.app.repository.BookmarkRepository
import com.browser.app.repository.HistoryRepository
import com.browser.app.repository.NoteRepository
import com.browser.app.repository.PasswordRepository
import com.browser.app.repository.RssRepository
import com.browser.app.repository.UserScriptRepository
import com.browser.app.repository.WindowRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.Executor

/**
 * WebAppInterface 单元测试（Robolectric 内存数据库 + 同步 Executor）。
 *
 * 验证 @JavascriptInterface 暴露的 bookmarks/history/notes/stats CRUD 方法：
 * - 读方法（runBlocking 同步返回）返回正确 JSON
 * - 写方法（launch 异步）最终落库
 * - saveNote 兼容方法仍可用
 *
 * 同步 Executor：让 Room suspend 操作在调用线程直接执行，
 * 配合 advanceUntilIdle 确保 launch 的协程完成，避免异步竞态。
 * 密码相关不测（Keystore 在 Robolectric 不可用）。
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class WebAppInterfaceTest {

    private lateinit var database: BrowserDatabase
    private lateinit var bookmarkRepo: BookmarkRepository
    private lateinit var historyRepo: HistoryRepository
    private lateinit var noteRepo: NoteRepository
    private lateinit var passwordRepo: PasswordRepository
    private lateinit var rssRepo: RssRepository
    private lateinit var userScriptRepo: UserScriptRepository
    private lateinit var windowRepo: WindowRepository
    private lateinit var webAppInterface: WebAppInterface

    @Before
    fun setup() {
        // 同步 Executor：让 Room 的 suspend 操作在调用线程直接执行，
        // 避免 advanceUntilIdle 无法等待 Room IO 线程
        val syncExecutor = Executor { it.run() }
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            BrowserDatabase::class.java
        )
            .allowMainThreadQueries()
            .setQueryExecutor(syncExecutor)
            .setTransactionExecutor(syncExecutor)
            .build()
        bookmarkRepo = BookmarkRepository(database.bookmarkDao())
        historyRepo = HistoryRepository(database.historyDao())
        noteRepo = NoteRepository(database.noteDao())
        passwordRepo = PasswordRepository(database.passwordDao())
        rssRepo = RssRepository(database.rssDao())
        userScriptRepo = UserScriptRepository(database.userScriptDao())
        windowRepo = WindowRepository(database.windowDao())
    }

    @After
    fun teardown() {
        database.close()
    }

    /** 用当前 TestScope 创建 WebAppInterface，launch 的协程由 advanceUntilIdle 驱动 */
    private fun createInterface(scope: CoroutineScope): WebAppInterface {
        return WebAppInterface(
            ApplicationProvider.getApplicationContext(),
            bookmarkRepo,
            historyRepo,
            noteRepo,
            passwordRepo,
            rssRepo,
            userScriptRepo,
            windowRepo,
            scope
        )
    }

    // ============ 书签 ============

    @Test
    fun getBookmarksJson_emptyInitially() = runTest {
        webAppInterface = createInterface(this)
        val arr = JSONArray(webAppInterface.getBookmarksJson())
        assertEquals(0, arr.length())
    }

    @Test
    fun addBookmark_writesToDatabase() = runTest {
        webAppInterface = createInterface(this)
        val result = webAppInterface.addBookmark("测试书签", "https://example.com")
        assertTrue("addBookmark 应返回 true", result)
        advanceUntilIdle()

        val all = bookmarkRepo.getAllBookmarks().first()
        assertEquals(1, all.size)
        assertEquals("测试书签", all[0].title)
        assertEquals("https://example.com", all[0].url)
    }

    @Test
    fun addBookmark_blankUrl_returnsFalse() = runTest {
        webAppInterface = createInterface(this)
        assertFalse("空 url 应返回 false", webAppInterface.addBookmark("标题", ""))
        advanceUntilIdle()
        assertEquals(0, bookmarkRepo.getAllBookmarks().first().size)
    }

    @Test
    fun removeBookmark_deletesFromDatabase() = runTest {
        webAppInterface = createInterface(this)
        webAppInterface.addBookmark("待删除", "https://delete.com")
        advanceUntilIdle()
        assertEquals(1, bookmarkRepo.getAllBookmarks().first().size)

        assertTrue(webAppInterface.removeBookmark("https://delete.com"))
        advanceUntilIdle()
        assertEquals(0, bookmarkRepo.getAllBookmarks().first().size)
    }

    @Test
    fun getBookmarksJson_returnsCorrectShape() = runTest {
        webAppInterface = createInterface(this)
        bookmarkRepo.addBookmark("标题A", "https://a.com")
        advanceUntilIdle()

        val arr = JSONArray(webAppInterface.getBookmarksJson())
        assertEquals(1, arr.length())
        val obj = arr.getJSONObject(0)
        assertEquals("标题A", obj.getString("title"))
        assertEquals("https://a.com", obj.getString("url"))
        assertTrue(obj.has("id"))
        assertTrue(obj.has("timestamp"))
        // folder 字段统一输出空串
        assertEquals("", obj.getString("folder"))
    }

    // ============ 历史 ============

    @Test
    fun getHistoryJson_emptyInitially() = runTest {
        webAppInterface = createInterface(this)
        val arr = JSONArray(webAppInterface.getHistoryJson())
        assertEquals(0, arr.length())
    }

    @Test
    fun addHistory_writesToDatabase() = runTest {
        webAppInterface = createInterface(this)
        assertTrue(webAppInterface.addHistory("页面", "https://history.com"))
        advanceUntilIdle()
        val all = historyRepo.getAllHistory().first()
        assertEquals(1, all.size)
        assertEquals("页面", all[0].title)
    }

    @Test
    fun clearHistory_emptiesDatabase() = runTest {
        webAppInterface = createInterface(this)
        historyRepo.addHistory("A", "https://a.com")
        historyRepo.addHistory("B", "https://b.com")
        advanceUntilIdle()
        assertEquals(2, historyRepo.getAllHistory().first().size)

        assertTrue(webAppInterface.clearHistory())
        advanceUntilIdle()
        assertEquals(0, historyRepo.getAllHistory().first().size)
    }

    @Test
    fun getHistoryJson_cappedAt100() = runTest {
        webAppInterface = createInterface(this)
        repeat(120) { i ->
            historyRepo.addHistory("页$i", "https://example.com/$i")
        }
        advanceUntilIdle()

        val arr = JSONArray(webAppInterface.getHistoryJson())
        assertEquals("getHistoryJson 应限制为最近 100 条", 100, arr.length())
    }

    // ============ 笔记 ============

    @Test
    fun getNotesJson_emptyInitially() = runTest {
        webAppInterface = createInterface(this)
        val arr = JSONArray(webAppInterface.getNotesJson())
        assertEquals(0, arr.length())
    }

    @Test
    fun addNote_writesToDatabase() = runTest {
        webAppInterface = createInterface(this)
        assertTrue(webAppInterface.addNote("笔记内容", "https://source.com", "来源标题"))
        advanceUntilIdle()
        val all = noteRepo.getAllNotes().first()
        assertEquals(1, all.size)
        assertEquals("笔记内容", all[0].text)
        assertEquals("https://source.com", all[0].sourceUrl)
        assertEquals("来源标题", all[0].sourceTitle)
    }

    @Test
    fun addNote_blankText_returnsFalse() = runTest {
        webAppInterface = createInterface(this)
        assertFalse(webAppInterface.addNote("", "https://source.com", "标题"))
        advanceUntilIdle()
        assertEquals(0, noteRepo.getAllNotes().first().size)
    }

    @Test
    fun deleteNote_removesFromDatabase() = runTest {
        webAppInterface = createInterface(this)
        val id = noteRepo.addNote("待删除笔记", "", "")
        advanceUntilIdle()
        assertEquals(1, noteRepo.getAllNotes().first().size)

        assertTrue(webAppInterface.deleteNote(id))
        advanceUntilIdle()
        assertEquals(0, noteRepo.getAllNotes().first().size)
    }

    @Test
    fun getNotesJson_mapsSourceTitleToSource() = runTest {
        webAppInterface = createInterface(this)
        noteRepo.addNote("文本", "https://src.com", "源标题")
        advanceUntilIdle()

        val arr = JSONArray(webAppInterface.getNotesJson())
        assertEquals(1, arr.length())
        val obj = arr.getJSONObject(0)
        assertEquals("文本", obj.getString("text"))
        // sourceTitle 映射为 Web 端的 source 字段
        assertEquals("源标题", obj.getString("source"))
        assertEquals("https://src.com", obj.getString("sourceUrl"))
    }

    // ============ 统计 ============

    @Test
    fun getStatsJson_allZeroInitially() = runTest {
        webAppInterface = createInterface(this)
        val obj = JSONObject(webAppInterface.getStatsJson())
        assertEquals(0, obj.getInt("bookmarks"))
        assertEquals(0, obj.getInt("history"))
        assertEquals(0, obj.getInt("notes"))
    }

    @Test
    fun getStatsJson_returnsCorrectCounts() = runTest {
        webAppInterface = createInterface(this)
        bookmarkRepo.addBookmark("B1", "https://b1.com")
        bookmarkRepo.addBookmark("B2", "https://b2.com")
        historyRepo.addHistory("H1", "https://h1.com")
        noteRepo.addNote("N1")
        noteRepo.addNote("N2")
        noteRepo.addNote("N3")
        advanceUntilIdle()

        val obj = JSONObject(webAppInterface.getStatsJson())
        assertEquals(2, obj.getInt("bookmarks"))
        assertEquals(1, obj.getInt("history"))
        assertEquals(3, obj.getInt("notes"))
    }

    // ============ 兼容方法 ============

    @Test
    fun saveNote_deprecatedStillWorks() = runTest {
        webAppInterface = createInterface(this)
        @Suppress("DEPRECATION")
        webAppInterface.saveNote("兼容笔记", "https://src.com", "标题")
        advanceUntilIdle()
        val all = noteRepo.getAllNotes().first()
        assertEquals(1, all.size)
        assertEquals("兼容笔记", all[0].text)
    }

    @Test
    fun getAppInfo_returnsVersionString() = runTest {
        webAppInterface = createInterface(this)
        val info = webAppInterface.getAppInfo()
        assertTrue("getAppInfo 应包含应用名", info.contains("MultiSearchBrowser"))
    }
}
