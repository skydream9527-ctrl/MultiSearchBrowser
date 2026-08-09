package com.browser.app.ui.home

import androidx.lifecycle.ViewModel
import com.browser.app.utils.PreferenceManager
import com.browser.app.utils.SearchEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val preferenceManager: PreferenceManager
) : ViewModel() {

    val searchEngines: List<SearchEngine> = SearchEngine.ALL

    val quickLinks: List<SearchEngine> = SearchEngine.ALL

    fun selectedEngine(): SearchEngine =
        SearchEngine.getById(preferenceManager.selectedSearchEngine)

    fun selectEngine(engine: SearchEngine) {
        preferenceManager.selectedSearchEngine = engine.id
    }

    /**
     * 把用户输入解析为最终 URL：http(s) 直达，否则走当前搜索引擎搜索。
     */
    fun buildSearchUrl(rawInput: String): String? {
        val query = rawInput.trim()
        if (query.isEmpty()) return null
        return if (query.startsWith("http://") || query.startsWith("https://")) {
            query
        } else {
            selectedEngine().searchUrl + java.net.URLEncoder.encode(query, "UTF-8")
        }
    }
}
