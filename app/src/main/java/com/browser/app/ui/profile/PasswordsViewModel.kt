package com.browser.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.browser.app.data.entity.PasswordEntity
import com.browser.app.repository.PasswordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 密码管理 ViewModel：暴露密码列表状态，封装添加与按 URL 查询明文密码操作。
 * Hilt 注入 PasswordRepository。
 */
@HiltViewModel
class PasswordsViewModel @Inject constructor(
    private val repository: PasswordRepository
) : ViewModel() {

    val passwords: StateFlow<List<PasswordEntity>> = repository.getAllPasswords()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addPassword(site: String, username: String, plainPassword: String, url: String = "") {
        viewModelScope.launch { repository.addPassword(site, username, plainPassword, url) }
    }

    suspend fun getPasswordByUrl(url: String): PasswordEntity? = repository.getByUrl(url)
}
