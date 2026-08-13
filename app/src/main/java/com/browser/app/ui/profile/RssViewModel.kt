package com.browser.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.browser.app.data.entity.RssItemEntity
import com.browser.app.repository.RssRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * RSS 列表 ViewModel：暴露 RSS 文章列表状态，封装标记已读与添加订阅源操作。
 * Hilt 注入 RssRepository。
 */
@HiltViewModel
class RssViewModel @Inject constructor(
    private val repository: RssRepository
) : ViewModel() {

    val items: StateFlow<List<RssItemEntity>> = repository.getAllItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun markAsRead(guid: String) {
        viewModelScope.launch { repository.markAsRead(guid) }
    }

    fun addFeed(name: String, url: String) {
        viewModelScope.launch { repository.addFeed(name, url) }
    }
}
