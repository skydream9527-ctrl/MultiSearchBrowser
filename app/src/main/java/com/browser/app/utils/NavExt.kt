package com.browser.app.utils

import androidx.navigation.NavController
import com.browser.app.R

/**
 * 统一入口：从任意 Fragment 跳转到 WebviewFragment。
 * 使用 nav_graph 中的全局 action，避免之前各 Fragment 越级调用
 * homeFragment 私有 action 导致的 IllegalArgumentException 崩溃。
 */
fun NavController.navigateToWebview(url: String) {
    navigate(R.id.action_global_webviewFragment, androidx.core.os.bundleOf("url" to url))
}
