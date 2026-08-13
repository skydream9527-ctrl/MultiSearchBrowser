package com.browser.app.ui.webview

import android.content.Context
import android.webkit.JavascriptInterface
import com.browser.app.repository.NoteRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * v2.0.0: WebView ↔ Native JS Bridge
 * 注入到 window.MSB 命名空间，供网页调用本地能力。
 *
 * 安全：
 * - 所有暴露给 JS 的方法必须显式标注 @JavascriptInterface
 * - WebviewFragment.onPageStarted 中按 origin 白名单注入，非信任域不注入
 * - 协程作用域由 Fragment 注入（viewLifecycleOwner），Fragment 销毁时自动取消
 */
class WebAppInterface(
    private val context: Context,
    private val noteRepository: NoteRepository,
    private val coroutineScope: CoroutineScope,
) {
    /** 网页调用保存笔记：MSB.saveNote(text, sourceUrl, sourceTitle) */
    @JavascriptInterface
    fun saveNote(text: String, sourceUrl: String, sourceTitle: String) {
        if (text.isBlank()) return
        coroutineScope.launch {
            noteRepository.addNote(text, sourceUrl, sourceTitle)
        }
    }

    /** 网页获取当前页标题（用于用户脚本/扩展） */
    @JavascriptInterface
    fun getAppInfo(): String = "MultiSearchBrowser/2.0.0 Android"

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
