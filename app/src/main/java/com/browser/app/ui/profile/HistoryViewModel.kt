package com.browser.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.browser.app.data.entity.HistoryEntity
import com.browser.app.repository.HistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 历史列表 ViewModel：暴露历史记录状态，封装删除与清空操作。
 * Hilt 注入 HistoryRepository。
 */
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: HistoryRepository
) : ViewModel() {

    val history: StateFlow<List<HistoryEntity>> = repository.getAllHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteHistory(entity: HistoryEntity) {
        viewModelScope.launch { repository.deleteHistory(entity) }
    }

    fun clearHistory() {
        viewModelScope.launch { repository.clearHistory() }
    }
}
