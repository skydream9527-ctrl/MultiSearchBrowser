package com.browser.app.utils

data class SearchEngine(
    val id: String,
    val name: String,
    val searchUrl: String,
    val colorResId: Int
) {
    companion object {
        val BAIDU = SearchEngine(
            id = "baidu",
            name = "百度",
            searchUrl = "https://www.baidu.com/s?wd=",
            colorResId = com.browser.app.R.color.baidu_blue
        )

        val SOGOU = SearchEngine(
            id = "sogou",
            name = "搜狗",
            searchUrl = "https://www.sogou.com/web?query=",
            colorResId = com.browser.app.R.color.sogou_orange
        )

        val BILIBILI = SearchEngine(
            id = "bilibili",
            name = "B站",
            searchUrl = "https://search.bilibili.com/all?keyword=",
            colorResId = com.browser.app.R.color.bilibili_pink
        )

        val DOUYIN = SearchEngine(
            id = "douyin",
            name = "抖音",
            searchUrl = "https://www.douyin.com/search/",
            colorResId = com.browser.app.R.color.douyin_black
        )

        val BING = SearchEngine(
            id = "bing",
            name = "必应",
            searchUrl = "https://www.bing.com/search?q=",
            colorResId = com.browser.app.R.color.bing_teal
        )

        val DOUBAO = SearchEngine(
            id = "doubao",
            name = "豆包",
            searchUrl = "https://www.doubao.com/search/",
            colorResId = com.browser.app.R.color.doubao_green
        )

        val QIANWEN = SearchEngine(
            id = "qianwen",
            name = "千问",
            searchUrl = "https://tongyi.aliyun.com/qianwen/?query=",
            colorResId = com.browser.app.R.color.qianwen_blue
        )

        val ALL = listOf(BAIDU, SOGOU, BILIBILI, DOUYIN, BING, DOUBAO, QIANWEN)

        fun getById(id: String): SearchEngine {
            return ALL.find { it.id == id } ?: BAIDU
        }
    }
}
