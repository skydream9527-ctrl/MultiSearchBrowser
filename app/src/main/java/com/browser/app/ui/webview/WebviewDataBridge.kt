package com.browser.app.ui.webview

import android.webkit.WebView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * v2.1.0: Android Room → Web 单向推送桥（C2）
 * 监听 Repository Flow 变化，通过 evaluateJavascript 通知 Web 端刷新。
 *
 * 设计：
 * - 注入 WebView 引用 + 协程作用域
 * - 订阅各 Repository 的 Flow，数据变化时调用 webview.evaluateJavascript
 * - 通知格式：window.MSBApp.onNativeDataChange('bookmarks')
 * - Web 端通过 MSBApp.onNativeDataChange 回调刷新对应 UI
 *
 * 安全：
 * - evaluateJavascript 在主线程执行
 * - 仅在 WebView 已 attached 时调用，避免 Fragment 销毁后崩溃
 *
 * 生命周期：
 * - coroutineScope 由 Fragment 注入（viewLifecycleOwner.lifecycleScope），Fragment 销毁自动取消
 *
 * 避免循环：
 * - Web 端通过 Bridge 写入 Room 会触发本 Flow，但 onNativeDataChange 只刷新 UI 不再回写 Room
 */
class WebviewDataBridge(
    private val webView: WebView,
    private val coroutineScope: CoroutineScope,
) {
    /** 启动所有数据源监听 */
    fun start(
        bookmarksFlow: Flow<List<com.browser.app.data.entity.BookmarkEntity>>,
        historyFlow: Flow<List<com.browser.app.data.entity.HistoryEntity>>,
        notesFlow: Flow<List<com.browser.app.data.entity.NoteEntity>>,
    ) {
        coroutineScope.launch {
            bookmarksFlow.collect { _ ->
                notifyWeb("bookmarks")
            }
        }
        coroutineScope.launch {
            historyFlow.collect { _ ->
                notifyWeb("history")
            }
        }
        coroutineScope.launch {
            notesFlow.collect { _ ->
                notifyWeb("notes")
            }
        }
    }

    /** 通过 evaluateJavascript 通知 Web 端刷新指定数据类型 */
    private fun notifyWeb(dataType: String) {
        if (webView.isAttachedToWindow) {
            webView.post {
                webView.evaluateJavascript(
                    "window.MSBApp && window.MSBApp.onNativeDataChange && window.MSBApp.onNativeDataChange('$dataType');",
                    null
                )
            }
        }
    }
}
