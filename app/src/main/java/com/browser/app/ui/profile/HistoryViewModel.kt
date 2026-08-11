package com.browser.app.ui.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.browser.app.R
import com.browser.app.data.entity.HistoryEntity
import com.browser.app.repository.HistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val historyRepository: HistoryRepository,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    /**
     * 按天分组后的历史列表（含分组标题）。
     * 搜索时只在内存中过滤，仍然保持分组结构，避免无谓的 DB 查询。
     */
    val items: StateFlow<List<HistoryListItem>> = combine(
        historyRepository.getAllHistory(),
        _searchQuery
    ) { all, query ->
        val filtered = if (query.isBlank()) {
            all
        } else {
            all.filter {
                it.title.contains(query, ignoreCase = true) ||
                    it.url.contains(query, ignoreCase = true)
            }
        }
        groupByDay(filtered)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun deleteHistory(item: HistoryEntity) {
        viewModelScope.launch {
            historyRepository.deleteHistory(item)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            historyRepository.clearHistory()
        }
    }

    /**
     * 把已按时间倒序排好的历史记录按"今天 / 昨天 / yyyy年MM月dd日"分组。
     * 因为 DAO 已经 ORDER BY timestamp DESC，这里直接顺序遍历即可。
     */
    private fun groupByDay(historyList: List<HistoryEntity>): List<HistoryListItem> {
        if (historyList.isEmpty()) return emptyList()

        val result = mutableListOf<HistoryListItem>()
        val now = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val todayStart = now.timeInMillis
        val yesterdayStart = (now.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, -1)
        }.timeInMillis

        val todayLabel = appContext.getString(R.string.history_group_today)
        val yesterdayLabel = appContext.getString(R.string.history_group_yesterday)
        val dayFmt = SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault())
        var prevBucket: String? = null

        historyList.forEach { history ->
            val ts = history.timestamp
            val bucket = when {
                ts >= todayStart -> todayLabel
                ts >= yesterdayStart -> yesterdayLabel
                else -> dayFmt.format(Date(ts))
            }
            if (bucket != prevBucket) {
                result.add(HistoryListItem.Header(bucket))
                prevBucket = bucket
            }
            result.add(HistoryListItem.Item(history))
        }
        return result
    }
}
