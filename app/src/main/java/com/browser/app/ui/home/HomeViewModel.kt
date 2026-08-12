package com.browser.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.browser.app.utils.PreferenceManager
import com.browser.app.utils.SearchEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 首页 ViewModel：管理当前选中的搜索引擎状态。
 * Hilt 注入 PreferenceManager，配置变更后自动恢复选中引擎。
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val preferenceManager: PreferenceManager
) : ViewModel() {

    private val _selectedEngine = MutableStateFlow(SearchEngine.getById(preferenceManager.selectedSearchEngine))
    val selectedEngine: StateFlow<SearchEngine> = _selectedEngine.asStateFlow()

    fun selectEngine(engine: SearchEngine) {
        viewModelScope.launch {
            preferenceManager.selectedSearchEngine = engine.id
            _selectedEngine.value = engine
        }
    }
}
