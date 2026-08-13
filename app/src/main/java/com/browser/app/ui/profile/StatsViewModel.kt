package com.browser.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.browser.app.repository.BookmarkRepository
import com.browser.app.repository.HistoryRepository
import com.browser.app.repository.NoteRepository
import com.browser.app.repository.RssRepository
import com.browser.app.repository.WindowRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.net.URL
import javax.inject.Inject

/**
 * 数据统计 ViewModel：聚合 5 个 Repository 的 Flow，输出统一 StatsUiState。
 * Hilt 注入 HistoryRepository + BookmarkRepository + WindowRepository + NoteRepository + RssRepository。
 */
@HiltViewModel
class StatsViewModel @Inject constructor(
    historyRepository: HistoryRepository,
    bookmarkRepository: BookmarkRepository,
    windowRepository: WindowRepository,
    noteRepository: NoteRepository,
    rssRepository: RssRepository
) : ViewModel() {

    data class StatsUiState(
        val historyCount: Int = 0,
        val bookmarkCount: Int = 0,
        val windowCount: Int = 0,
        val noteCount: Int = 0,
        val rssCount: Int = 0,
        val topSites: List<Pair<String, Int>> = emptyList()
    )

    private val historyListFlow = historyRepository.getAllHistory()
    private val bookmarkCountFlow = bookmarkRepository.getAllBookmarks().map { it.size }
    private val windowCountFlow = windowRepository.getCount()
    private val noteCountFlow = noteRepository.getCount()
    private val rssCountFlow = rssRepository.getAllItems().map { it.size }

    val uiState: StateFlow<StatsUiState> = combine(
        historyListFlow,
        bookmarkCountFlow,
        windowCountFlow,
        noteCountFlow,
        rssCountFlow
    ) { historyList, bookmarkCount, windowCount, noteCount, rssCount ->
        StatsUiState(
            historyCount = historyList.size,
            bookmarkCount = bookmarkCount,
            windowCount = windowCount,
            noteCount = noteCount,
            rssCount = rssCount,
            topSites = computeTopSites(historyList.map { it.url })
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatsUiState())

    private fun computeTopSites(urls: List<String>, limit: Int = 10): List<Pair<String, Int>> {
        return urls.mapNotNull { runCatching { URL(it).host }.getOrNull() }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(limit)
            .map { it.key to it.value }
    }
}
