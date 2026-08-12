package com.browser.app.ui.tabs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.browser.app.data.entity.WindowEntity
import com.browser.app.repository.WindowRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 多窗口 ViewModel：暴露窗口列表状态，封装新建与关闭操作。
 * Hilt 注入 WindowRepository。
 */
@HiltViewModel
class WindowsViewModel @Inject constructor(
    private val repository: WindowRepository
) : ViewModel() {

    val windows: StateFlow<List<WindowEntity>> = repository.getAllWindows()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    suspend fun addWindow(title: String, url: String): Long {
        return repository.addWindow(title, url)
    }

    fun deleteWindow(window: WindowEntity) {
        viewModelScope.launch { repository.deleteWindow(window) }
    }
}
