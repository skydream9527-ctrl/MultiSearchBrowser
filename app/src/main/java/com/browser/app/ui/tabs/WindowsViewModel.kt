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

@HiltViewModel
class WindowsViewModel @Inject constructor(
    private val windowRepository: WindowRepository
) : ViewModel() {

    val windows: StateFlow<List<WindowEntity>> = windowRepository.getAllWindows()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val count: StateFlow<Int> = windowRepository.getCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun deleteWindow(window: WindowEntity) {
        viewModelScope.launch {
            windowRepository.deleteWindow(window)
        }
    }
}
