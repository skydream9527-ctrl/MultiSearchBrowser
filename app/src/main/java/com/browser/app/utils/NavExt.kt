package com.browser.app.utils

import androidx.navigation.NavController
import com.browser.app.ui.webview.WebviewFragmentDirections

/**
 * 统一入口：从任意 Fragment 跳转到 WebviewFragment。
 * 使用 Safe Args 生成的 WebviewFragmentDirections 替代手动 bundleOf，
 * 保证参数类型安全，编译期即可发现参数缺失或类型错误。
 *
 * @param windowId 关联的多窗口 id，-1 表示不关联（来自首页/历史/书签的临时浏览）
 */
fun NavController.navigateToWebview(url: String, windowId: Long = -1L) {
    navigate(WebviewFragmentDirections.actionGlobalWebviewFragment(url, windowId))
}
