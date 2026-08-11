package com.browser.app.ui.home

import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import com.browser.app.R

/**
 * 首页"常用网站"快捷入口：与搜索引擎区分，
 * 这些是直接打开 URL 的快捷方式（而非搜索框 query）。
 */
data class QuickSite(
    @StringRes val nameResId: Int,
    val url: String,
    @ColorRes val colorResId: Int
) {
    companion object {
        val DEFAULTS: List<QuickSite> = listOf(
            QuickSite(R.string.site_weibo, "https://m.weibo.cn", R.color.baidu_blue),
            QuickSite(R.string.site_zhihu, "https://www.zhihu.com", R.color.bing_teal),
            QuickSite(R.string.site_taobao, "https://m.taobao.com", R.color.sogou_orange),
            QuickSite(R.string.site_jd, "https://m.jd.com", R.color.doubao_green),
            QuickSite(R.string.site_bilibili, "https://m.bilibili.com", R.color.bilibili_pink),
            QuickSite(R.string.site_douyin, "https://m.douyin.com", R.color.douyin_black),
            QuickSite(R.string.site_github, "https://github.com", R.color.text_primary),
            QuickSite(R.string.site_wikipedia, "https://zh.m.wikipedia.org", R.color.qianwen_blue)
        )
    }
}
