package com.browser.app.webview

import android.webkit.WebView

/**
 * 低版本 WebView（API < 29，无 `WebSettings.setForceDark`）的夜间模式兼容方案：
 * 通过 evaluateJavascript 注入一段 CSS，把页面背景/前景翻转为深色。
 *
 * 限制：
 * - 只能影响 DOM 已构建完成后的样式，对早于 [android.webkit.WebViewClient.onPageFinished]
 *   阶段的内容无效；
 * - 对图片/视频/iframe 内部内容不做处理；
 * - 不能保证 100% 网页可读，复杂站点（白名单类）效果有限。
 *
 * 调用时机：[android.webkit.WebViewClient.onPageFinished] 中，仅当用户开启夜间模式
 * 且系统不支持 forceDark 时调用。
 */
object DarkModeCssInjector {

    /** 注入到 <head> 的样式 ID，便于重复注入前先移除避免叠加。 */
    private const val STYLE_ID = "msb_dark_mode_css"

    /**
     * 注意：CSS 文本必须转义成 JS 字符串字面量，避免换行/引号/反斜杠破坏 JS。
     * 这里直接用反引号字符串字面量更稳，但低版本 WebView 对模板字符串支持不一致，
     * 因此用普通双引号字符串 + 显式转义。
     */
    private val CSS = """
        html {
            filter: invert(1) hue-rotate(180deg) !important;
            background: #111 !important;
        }
        img, picture, video, iframe, canvas, svg, embed, object {
            filter: invert(1) hue-rotate(180deg) !important;
        }
        /* 防止文字过亮 */
        body { background: #111 !important; }
    """.trimIndent()

    private val JS_TEMPLATE = """
        (function() {
            var existing = document.getElementById('$STYLE_ID');
            if (existing) { existing.remove(); }
            var style = document.createElement('style');
            style.id = '$STYLE_ID';
            style.type = 'text/css';
            style.appendChild(document.createTextNode(%s));
            var head = document.head || document.getElementsByTagName('head')[0];
            if (head) { head.appendChild(style); } else {
                var body = document.body || document.getElementsByTagName('body')[0];
                if (body) { body.appendChild(style); }
            }
        })();
    """.trimIndent()

    /**
     * 把 CSS 文本转义成 JS 字符串字面量（用单引号包裹），然后套入 [JS_TEMPLATE]。
     * 使用单引号是因为 CSS 内部双引号较多；转义反斜杠/换行/单引号即可。
     */
    private fun buildJs(): String {
        val escaped = CSS
            .replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\n", "\\n")
        return JS_TEMPLATE.format("'$escaped'")
    }

    /** 注入夜间模式 CSS，幂等可重复调用。 */
    fun inject(webView: WebView) {
        try {
            webView.evaluateJavascript(buildJs(), null)
        } catch (_: Throwable) {
            // 低版本 WebView 在某些状态下 evaluateJavascript 可能抛异常，忽略即可
        }
    }

    /** 关闭夜间模式：移除已注入的 <style>。 */
    fun remove(webView: WebView) {
        try {
            webView.evaluateJavascript(
                """
                (function() {
                    var el = document.getElementById('$STYLE_ID');
                    if (el) { el.remove(); }
                })();
                """.trimIndent(),
                null
            )
        } catch (_: Throwable) {
            // 同上忽略
        }
    }
}
