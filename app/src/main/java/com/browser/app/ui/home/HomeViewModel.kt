package com.browser.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.browser.app.repository.HistoryRepository
import com.browser.app.utils.PreferenceManager
import com.browser.app.utils.SearchEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val preferenceManager: PreferenceManager,
    private val historyRepository: HistoryRepository
) : ViewModel() {

    val searchEngines: List<SearchEngine> = SearchEngine.ALL

    val quickLinks: List<SearchEngine> = SearchEngine.ALL

    private val _suggestions = MutableStateFlow<List<SearchSuggestion>>(emptyList())
    val suggestions: StateFlow<List<SearchSuggestion>> = _suggestions.asStateFlow()

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

    /**
     * 根据当前输入实时生成搜索建议：
     * - 第一条永远是 "用当前引擎搜索此关键词"
     * - 后续是历史里 title / url 命中的记录（去重，最多 8 条）
     */
    fun updateSuggestions(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            _suggestions.value = emptyList()
            return
        }
        viewModelScope.launch {
            val historyMatches = historyRepository.searchHistory(trimmed)
                .distinctBy { it.url }
                .take(8)
                .map { SearchSuggestion.History(it.title.ifBlank { it.url }, it.url) }
            // 顶部固定一条 "用 {engine} 搜索 {query}" 的建议
            val engine = selectedEngine()
            // displayText 仅作为 UI 提示用，点击行为用 query 字段单独承载
            val direct = SearchSuggestion.DirectSearch(
                displayText = "「${engine.id}」搜索 $trimmed",
                query = trimmed,
                engine = engine
            )
            _suggestions.value = listOf(direct) + historyMatches
        }
    }

    fun clearSuggestions() {
        _suggestions.value = emptyList()
    }

    /**
         * 建议项的统一模型，UI 据此渲染点击行为
         */
        sealed class SearchSuggestion {
            abstract val displayText: String
            /** 直接用当前引擎搜索关键词 */
            data class DirectSearch(
                override val displayText: String,
                val query: String,
                val engine: SearchEngine
            ) : SearchSuggestion()
            /** 历史 / 收藏里命中的页面，点击直接打开 url */
            data class History(
                override val displayText: String,
                val url: String
            ) : SearchSuggestion()
        }
}
