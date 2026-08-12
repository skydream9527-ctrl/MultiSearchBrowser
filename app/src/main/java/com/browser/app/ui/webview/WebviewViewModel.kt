package com.browser.app.ui.webview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.browser.app.repository.BookmarkRepository
import com.browser.app.repository.HistoryRepository
import com.browser.app.repository.WindowRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * WebView ViewModel：封装书签切换、历史写入、窗口回写操作。
 * 替代原 WebviewFragment 中直接创建 3 个 Repository 的逻辑。
 * Hilt 注入所有依赖。
 */
@HiltViewModel
class WebviewViewModel @Inject constructor(
    private val historyRepository: HistoryRepository,
    private val bookmarkRepository: BookmarkRepository,
    private val windowRepository: WindowRepository
) : ViewModel() {

    /** 观察某 URL 是否已收藏 */
    fun isBookmarked(url: String): Flow<Boolean> = bookmarkRepository.isBookmarked(url)

    /** 切换收藏状态，返回 true=已添加 false=已取消 */
    suspend fun toggleBookmark(title: String, url: String): Boolean =
        bookmarkRepository.toggleBookmark(title, url)

    /** 写入浏览历史（UPSERT 语义） */
    fun addHistory(title: String, url: String) {
        viewModelScope.launch { historyRepository.addHistory(title, url) }
    }

    /** 回写窗口的 url 和 title */
    fun updateWindow(windowId: Long, url: String, title: String) {
        viewModelScope.launch {
            val existing = windowRepository.getWindowById(windowId) ?: return@launch
            windowRepository.updateWindow(
                existing.copy(url = url, title = title, timestamp = System.currentTimeMillis())
            )
        }
    }
}
