package com.browser.app.utils

import androidx.navigation.NavController
import com.browser.app.R

/**
 * 统一入口：从任意 Fragment 跳转到 WebviewFragment。
 * 使用 nav_graph 中的全局 action，避免之前各 Fragment 越级调用
 * homeFragment 私有 action 导致的 IllegalArgumentException 崩溃。
 *
 * @param windowId 关联的多窗口 id，-1 表示不关联（来自首页/历史/书签的临时浏览）
 */
fun NavController.navigateToWebview(url: String, windowId: Long = -1L) {
    navigate(
        R.id.action_global_webviewFragment,
        androidx.core.os.bundleOf(
            "url" to url,
            "windowId" to windowId
        )
    )
}
