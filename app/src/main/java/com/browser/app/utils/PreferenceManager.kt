package com.browser.app.utils

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferenceManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("browser_prefs", Context.MODE_PRIVATE)

    // ---------- 已有字段 ----------
    var selectedSearchEngine: String
        get() = prefs.getString(KEY_SEARCH_ENGINE, "baidu") ?: "baidu"
        set(value) = prefs.edit().putString(KEY_SEARCH_ENGINE, value).apply()

    var avatarUri: String?
        get() = prefs.getString(KEY_AVATAR_URI, null)
        set(value) = prefs.edit().putString(KEY_AVATAR_URI, value).apply()

    var currentWindowId: Long
        get() = prefs.getLong(KEY_CURRENT_WINDOW_ID, 0)
        set(value) = prefs.edit().putLong(KEY_CURRENT_WINDOW_ID, value).apply()

    // ---------- 新增：浏览设置 ----------
    /** 默认 UA：mobile / desktop */
    var defaultUserAgent: String
        get() = prefs.getString(KEY_DEFAULT_UA, "mobile") ?: "mobile"
        set(value) = prefs.edit().putString(KEY_DEFAULT_UA, value).apply()

    /** JavaScript 开关，默认开 */
    var javaScriptEnabled: Boolean
        get() = prefs.getBoolean(KEY_JS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_JS_ENABLED, value).apply()

    /** Cookie 开关，默认开 */
    var cookieEnabled: Boolean
        get() = prefs.getBoolean(KEY_COOKIE_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_COOKIE_ENABLED, value).apply()

    /** 第三方 Cookie 开关，默认关 */
    var thirdPartyCookieEnabled: Boolean
        get() = prefs.getBoolean(KEY_THIRD_PARTY_COOKIE, false)
        set(value) = prefs.edit().putBoolean(KEY_THIRD_PARTY_COOKIE, value).apply()

    /** 阻止混合内容（https 页加载 http 资源），默认开 */
    var blockMixedContent: Boolean
        get() = prefs.getBoolean(KEY_BLOCK_MIXED_CONTENT, true)
        set(value) = prefs.edit().putBoolean(KEY_BLOCK_MIXED_CONTENT, value).apply()

    /** 夜间模式：强制暗黑网页，默认关 */
    var nightMode: Boolean
        get() = prefs.getBoolean(KEY_NIGHT_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_NIGHT_MODE, value).apply()

    companion object {
        private const val KEY_SEARCH_ENGINE = "search_engine"
        private const val KEY_AVATAR_URI = "avatar_uri"
        private const val KEY_CURRENT_WINDOW_ID = "current_window_id"
        private const val KEY_DEFAULT_UA = "default_ua"
        private const val KEY_JS_ENABLED = "js_enabled"
        private const val KEY_COOKIE_ENABLED = "cookie_enabled"
        private const val KEY_THIRD_PARTY_COOKIE = "third_party_cookie"
        private const val KEY_BLOCK_MIXED_CONTENT = "block_mixed_content"
        private const val KEY_NIGHT_MODE = "night_mode"
    }
}
