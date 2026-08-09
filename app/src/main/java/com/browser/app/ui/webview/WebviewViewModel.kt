package com.browser.app.ui.webview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.browser.app.repository.BookmarkRepository
import com.browser.app.repository.HistoryRepository
import com.browser.app.repository.WindowRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WebviewViewModel @Inject constructor(
    private val historyRepository: HistoryRepository,
    private val bookmarkRepository: BookmarkRepository,
    private val windowRepository: WindowRepository
) : ViewModel() {

    private val _pageState = MutableStateFlow(PageState())
    val pageState: StateFlow<PageState> = _pageState.asStateFlow()

    /** 当前 tab 在 DB 中的 id，0 表示尚未持久化 */
    private val _currentWindowId = MutableStateFlow(0L)
    val currentWindowId: StateFlow<Long> = _currentWindowId.asStateFlow()

    /**
     * 监听某 URL 是否已收藏，UI 通过 repeatOnLifecycle 收集。
     */
    fun isBookmarked(url: String): StateFlow<Boolean> =
        bookmarkRepository.isBookmarked(url)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /**
     * 进入 WebviewFragment 时调用：
     * - windowId > 0：从 DB 加载已存在的 tab，返回其 url 作为加载入口
     * - windowId == 0：用传入的 url 新建一个 tab，返回 url
     */
    suspend fun initTab(windowId: Long, fallbackUrl: String): String {
        return if (windowId > 0) {
            val entity = windowRepository.getWindowById(windowId)
            _currentWindowId.value = windowId
            val url = entity?.url ?: fallbackUrl
            _pageState.value = PageState(title = entity?.title ?: "", url = url)
            url
        } else {
            val newId = windowRepository.addWindow(title = "", url = fallbackUrl)
            _currentWindowId.value = newId
            _pageState.value = PageState(title = "", url = fallbackUrl)
            fallbackUrl
        }
    }

    fun onPageFinished(title: String, url: String) {
        _pageState.value = _pageState.value.copy(title = title, url = url)
        val windowId = _currentWindowId.value
        viewModelScope.launch {
            // 写浏览历史
            historyRepository.addHistory(title, url)
            // 同步更新当前 tab 的 url + title，离开后 WindowsFragment 列表能反映最新状态
            if (windowId > 0) {
                val existing = windowRepository.getWindowById(windowId)
                if (existing != null) {
                    windowRepository.updateWindow(
                        existing.copy(title = title.ifEmpty { url }, url = url)
                    )
                }
            }
        }
    }

    fun toggleBookmark(title: String, url: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val added = bookmarkRepository.toggleBookmark(title, url)
            onResult(added)
        }
    }

    data class PageState(
        val title: String = "",
        val url: String = ""
    )
}
