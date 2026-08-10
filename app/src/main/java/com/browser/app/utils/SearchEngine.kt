package com.browser.app.utils

import androidx.annotation.StringRes

data class SearchEngine(
    val id: String,
    @StringRes val nameResId: Int,
    val searchUrl: String,
    val colorResId: Int
) {
    companion object {
        val BAIDU = SearchEngine(
            id = "baidu",
            nameResId = com.browser.app.R.string.search_engine_baidu,
            searchUrl = "https://www.baidu.com/s?wd=",
            colorResId = com.browser.app.R.color.baidu_blue
        )

        val SOGOU = SearchEngine(
            id = "sogou",
            nameResId = com.browser.app.R.string.search_engine_sogou,
            searchUrl = "https://www.sogou.com/web?query=",
            colorResId = com.browser.app.R.color.sogou_orange
        )

        val BILIBILI = SearchEngine(
            id = "bilibili",
            nameResId = com.browser.app.R.string.search_engine_bilibili,
            searchUrl = "https://search.bilibili.com/all?keyword=",
            colorResId = com.browser.app.R.color.bilibili_pink
        )

        val DOUYIN = SearchEngine(
            id = "douyin",
            nameResId = com.browser.app.R.string.search_engine_douyin,
            searchUrl = "https://www.douyin.com/search/",
            colorResId = com.browser.app.R.color.douyin_black
        )

        val BING = SearchEngine(
            id = "bing",
            nameResId = com.browser.app.R.string.search_engine_bing,
            searchUrl = "https://www.bing.com/search?q=",
            colorResId = com.browser.app.R.color.bing_teal
        )

        val DOUBAO = SearchEngine(
            id = "doubao",
            nameResId = com.browser.app.R.string.search_engine_doubao,
            searchUrl = "https://www.doubao.com/search/",
            colorResId = com.browser.app.R.color.doubao_green
        )

        val QIANWEN = SearchEngine(
            id = "qianwen",
            nameResId = com.browser.app.R.string.search_engine_qianwen,
            searchUrl = "https://tongyi.aliyun.com/qianwen/?query=",
            colorResId = com.browser.app.R.color.qianwen_blue
        )

        val ALL = listOf(BAIDU, SOGOU, BILIBILI, DOUYIN, BING, DOUBAO, QIANWEN)

        fun getById(id: String): SearchEngine {
            return ALL.find { it.id == id } ?: BAIDU
        }
    }
}
