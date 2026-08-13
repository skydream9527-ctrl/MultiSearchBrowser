package com.browser.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.browser.app.data.entity.UserScriptEntity
import com.browser.app.repository.UserScriptRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 用户脚本 ViewModel：暴露脚本列表状态，封装添加与切换启用状态操作。
 * Hilt 注入 UserScriptRepository。
 */
@HiltViewModel
class UserScriptsViewModel @Inject constructor(
    private val repository: UserScriptRepository
) : ViewModel() {

    val scripts: StateFlow<List<UserScriptEntity>> = repository.getAllScripts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addScript(name: String, pattern: String, code: String) {
        viewModelScope.launch { repository.addScript(name, pattern, code) }
    }

    fun toggleEnabled(script: UserScriptEntity) {
        viewModelScope.launch { repository.setEnabled(script.id, !script.enabled) }
    }
}
