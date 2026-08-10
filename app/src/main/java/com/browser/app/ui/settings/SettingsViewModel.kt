package com.browser.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.browser.app.repository.HistoryRepository
import com.browser.app.utils.PreferenceManager
import com.browser.app.webview.WebViewPool
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferenceManager: PreferenceManager,
    private val historyRepository: HistoryRepository,
    private val webViewPool: WebViewPool
) : ViewModel() {

    // ---------- 读 ----------
    fun selectedEngineId(): String = preferenceManager.selectedSearchEngine
    fun defaultUserAgent(): String = preferenceManager.defaultUserAgent
    fun isJavaScriptEnabled(): Boolean = preferenceManager.javaScriptEnabled
    fun isCookieEnabled(): Boolean = preferenceManager.cookieEnabled
    fun isThirdPartyCookieEnabled(): Boolean = preferenceManager.thirdPartyCookieEnabled
    fun isBlockMixedContent(): Boolean = preferenceManager.blockMixedContent
    fun isNightMode(): Boolean = preferenceManager.nightMode

    // ---------- 写 ----------
    fun setSearchEngine(engineId: String) {
        preferenceManager.selectedSearchEngine = engineId
    }

    fun setDefaultUserAgent(value: String) {
        preferenceManager.defaultUserAgent = value
    }

    fun setJavaScriptEnabled(value: Boolean) {
        preferenceManager.javaScriptEnabled = value
    }

    fun setCookieEnabled(value: Boolean) {
        preferenceManager.cookieEnabled = value
    }

    fun setThirdPartyCookieEnabled(value: Boolean) {
        preferenceManager.thirdPartyCookieEnabled = value
    }

    fun setBlockMixedContent(value: Boolean) {
        preferenceManager.blockMixedContent = value
    }

    fun setNightMode(value: Boolean) {
        preferenceManager.nightMode = value
    }

    /**
     * 清除浏览数据：历史 + 所有 Cookie + 所有 WebView 缓存。
     * 注意：清缓存后已打开的 WebView 当前页面不会受影响，仅清空磁盘/内存缓存。
     */
    fun clearBrowsingData(onDone: () -> Unit) {
        viewModelScope.launch {
            historyRepository.clearHistory()
            // CookieManager 的 removeAllCookies 是异步回调，这里 fire-and-forget
            android.webkit.CookieManager.getInstance().removeAllCookies(null)
            android.webkit.CookieManager.getInstance().flush()
            android.webkit.WebStorage.getInstance().deleteAllData()
            webViewPool.clearCache()
            onDone()
        }
    }
}
