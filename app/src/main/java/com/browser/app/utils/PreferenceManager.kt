package com.browser.app.utils

import android.content.Context
import android.content.SharedPreferences

class PreferenceManager(context: Context) {
    private val prefs: SharedPreferences = 
        context.getSharedPreferences("browser_prefs", Context.MODE_PRIVATE)

    var selectedSearchEngine: String
        get() = prefs.getString("search_engine", "baidu") ?: "baidu"
        set(value) = prefs.edit().putString("search_engine", value).apply()

    var avatarUri: String?
        get() = prefs.getString("avatar_uri", null)
        set(value) = prefs.edit().putString("avatar_uri", value).apply()

    var currentWindowId: Long
        get() = prefs.getLong("current_window_id", 0)
        set(value) = prefs.edit().putLong("current_window_id", value).apply()
}
