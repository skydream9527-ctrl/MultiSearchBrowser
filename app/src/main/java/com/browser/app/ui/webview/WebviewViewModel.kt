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

    /** 当前 tab 是否无痕模式：无痕时不写历史、关闭 Cookie */
    private val _isIncognito = MutableStateFlow(false)
    val isIncognito: StateFlow<Boolean> = _isIncognito.asStateFlow()

    fun isBookmarked(url: String): StateFlow<Boolean> =
        bookmarkRepository.isBookmarked(url)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /**
     * 进入 WebviewFragment 时调用：
     * - windowId > 0：从 DB 加载已存在的 tab，返回其 url 作为加载入口
     * - windowId == 0：用传入的 url 新建一个 tab，返回 url
     *
     * @param incognito 仅在 windowId == 0（新建 tab）时生效，决定是否创建无痕窗口
     */
    suspend fun initTab(windowId: Long, fallbackUrl: String, incognito: Boolean): String {
        return if (windowId > 0) {
            val entity = windowRepository.getWindowById(windowId)
            _currentWindowId.value = windowId
            _isIncognito.value = entity?.isIncognito == true
            val url = entity?.url ?: fallbackUrl
            _pageState.value = PageState(title = entity?.title ?: "", url = url)
            url
        } else {
            val newId = windowRepository.addWindow(
                title = "",
                url = fallbackUrl,
                isIncognito = incognito
            )
            _currentWindowId.value = newId
            _isIncognito.value = incognito
            _pageState.value = PageState(title = "", url = fallbackUrl)
            fallbackUrl
        }
    }

    fun onPageFinished(title: String, url: String) {
        _pageState.value = _pageState.value.copy(title = title, url = url)
        val windowId = _currentWindowId.value
        viewModelScope.launch {
            // 无痕模式不写历史
            if (!_isIncognito.value) {
                historyRepository.addHistory(title, url)
            }
            // 无论是否无痕，都同步更新当前 tab 的 url + title（无痕 tab 也需要在窗口列表里显示）
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
