package com.browser.app.ui.webview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.browser.app.repository.BookmarkRepository
import com.browser.app.repository.HistoryRepository
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
    private val bookmarkRepository: BookmarkRepository
) : ViewModel() {

    private val _pageState = MutableStateFlow(PageState())
    val pageState: StateFlow<PageState> = _pageState.asStateFlow()

    /**
     * 监听某 URL 是否已收藏，UI 通过 repeatOnLifecycle 收集。
     */
    fun isBookmarked(url: String): StateFlow<Boolean> =
        bookmarkRepository.isBookmarked(url)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun onPageFinished(title: String, url: String) {
        _pageState.value = _pageState.value.copy(title = title, url = url)
        viewModelScope.launch {
            historyRepository.addHistory(title, url)
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
