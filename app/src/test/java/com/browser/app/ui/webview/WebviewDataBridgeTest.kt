package com.browser.app.ui.webview

import android.webkit.WebView
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowWebView

/**
 * WebviewDataBridge 单元测试（C2）。
 *
 * 任务约束：Robolectric 对 WebView.evaluateJavascript 支持有限，
 * 仅测 notifyWeb 在 webView.isAttachedToWindow == false 时不崩溃，
 * 并验证未触发 evaluateJavascript 调用。
 *
 * Robolectric 默认 WebView 未 attached，isAttachedToWindow == false，
 * 因此 notifyWeb 应直接 return，不调用 evaluateJavascript。
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class WebviewDataBridgeTest {

    private lateinit var webView: WebView

    @Before
    fun setup() {
        webView = WebView(ApplicationProvider.getApplicationContext())
    }

    @Test
    fun start_whenWebViewNotAttached_doesNotCrashOrEvaluate() = runTest {
        // Robolectric 默认 WebView 未 attached
        assertFalse("预置：WebView 默认未 attached", webView.isAttachedToWindow)

        val bookmarksFlow = MutableStateFlow<List<com.browser.app.data.entity.BookmarkEntity>>(emptyList())
        val historyFlow = MutableStateFlow<List<com.browser.app.data.entity.HistoryEntity>>(emptyList())
        val notesFlow = MutableStateFlow<List<com.browser.app.data.entity.NoteEntity>>(emptyList())

        val bridge = WebviewDataBridge(webView, this)
        bridge.start(bookmarksFlow, historyFlow, notesFlow)

        // 触发各 Flow 变更，应不崩溃且不调用 evaluateJavascript
        bookmarksFlow.value = listOf(
            com.browser.app.data.entity.BookmarkEntity(title = "B", url = "https://b.com")
        )
        historyFlow.value = listOf(
            com.browser.app.data.entity.HistoryEntity(title = "H", url = "https://h.com")
        )
        notesFlow.value = listOf(
            com.browser.app.data.entity.NoteEntity(text = "N")
        )
        advanceUntilIdle()

        // Robolectric ShadowWebView 提供最近一次 evaluateJavascript 记录；未 attached 时应为空
        val evaluated = ShadowWebView.getLastEvaluatedJavascript(webView)
        assertTrue(
            "WebView 未 attached 时不应调用 evaluateJavascript",
            evaluated.isNullOrBlank()
        )
    }
}
