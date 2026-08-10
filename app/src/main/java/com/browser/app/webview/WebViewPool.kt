package com.browser.app.webview

import android.content.Context
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import com.browser.app.utils.PreferenceManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 应用级 WebView 池：多窗口"真复用"的关键。
 *
 * 每个 windowId 对应一个长期持有的 WebView，Fragment 切换时只是从容器
 * add/removeView，不销毁 WebView，切回去后页面状态、滚动位置、表单输入都保留。
 *
 * 注意：WebView 用 ApplicationContext 创建，避免随 Activity 一起被回收；
 * 但 WebChromeClient 弹窗 / 文件选择仍由 Fragment 通过 Activity Result 处理。
 *
 * WebView 的默认配置由 [PreferenceManager] 提供，每次 obtain 时都重新应用一次，
 * 让用户在 SettingsFragment 改完设置切回 tab 立即生效。
 */
@Singleton
class WebViewPool @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferenceManager: PreferenceManager
) {
    private val cache = mutableMapOf<Long, WebView>()

    /**
     * 获取 [windowId] 对应的 WebView，不存在就新建并应用默认配置；
     * 已存在则重新应用一次 prefs（让设置变更立即生效）。
     * 调用方负责把它 add 进自己的容器、设置 WebViewClient / WebChromeClient / DownloadListener。
     */
    @Synchronized
    fun obtain(windowId: Long): WebView {
        return cache.getOrPut(windowId) {
            WebView(context).apply { configureDefaults() }
        }.also { applyPreferences(it) }
    }

    /** 释放单个 tab：销毁 WebView 并从池中移除，避免内存泄漏。 */
    @Synchronized
    fun release(windowId: Long) {
        cache.remove(windowId)?.let { webView ->
            try {
                webView.stopLoading()
                // SDK 把 WebViewClient/WebChromeClient 的 setter 标为 @NonNull，
                // 不能传 null；用空实现占位，避免 destroy 过程中回调到旧 Fragment 的 binding。
                webView.webViewClient = object : android.webkit.WebViewClient() {}
                webView.webChromeClient = android.webkit.WebChromeClient()
                (webView.parent as? ViewGroup)?.removeView(webView)
                webView.destroy()
            } catch (_: Throwable) {
                // 忽略 destroy 异常，保证不会因为单个 WebView 异常影响整体清理
            }
        }
    }

    /** 应用退出时调用：清理所有 WebView。 */
    @Synchronized
    fun releaseAll() {
        cache.keys.toList().forEach(::release)
    }

    /** 清空所有 WebView 的缓存（磁盘 + 内存），不影响已加载页面。 */
    @Synchronized
    fun clearCache() {
        cache.values.forEach { webView ->
            try {
                webView.clearCache(true)
            } catch (_: Throwable) {
                // 忽略单个异常
            }
        }
    }

    /** 应用持久化的默认配置：仅在新创建 WebView 时调用一次。 */
    private fun WebView.configureDefaults() {
        // 基础设置：与用户偏好无关的部分
        settings.apply {
            domStorageEnabled = true
            databaseEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            builtInZoomControls = true
            displayZoomControls = false
            cacheMode = WebSettings.LOAD_DEFAULT
            allowFileAccess = false
            allowContentAccess = true
            mediaPlaybackRequiresUserGesture = true
        }
        // 用户偏好相关：第一次创建时也走 applyPreferences
        applyPreferences(this@configureDefaults)
    }

    /** 读取 PreferenceManager 并应用到 WebView，每次 obtain 都执行一次让设置即时生效。 */
    private fun applyPreferences(webView: WebView) {
        webView.settings.apply {
            javaScriptEnabled = preferenceManager.javaScriptEnabled
            // MIXED_CONTENT_NEVER_ALLOW 对应"阻止混合内容 = true"
            mixedContentMode = if (preferenceManager.blockMixedContent) {
                WebSettings.MIXED_CONTENT_NEVER_ALLOW
            } else {
                WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            }
            // 夜间模式：Android Q+ 用 setForceDark，低版本暂不支持（避免反射兼容性问题）
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                webView.settings.forceDark = if (preferenceManager.nightMode) {
                    WebSettings.FORCE_DARK_ON
                } else {
                    WebSettings.FORCE_DARK_OFF
                }
            }
        }
        // Cookie：池级统一管理，所有 WebView 共享同一 CookieManager
        CookieManager.getInstance().setAcceptCookie(preferenceManager.cookieEnabled)
        CookieManager.getInstance().setAcceptThirdPartyCookies(
            webView,
            preferenceManager.thirdPartyCookieEnabled
        )
    }
}
