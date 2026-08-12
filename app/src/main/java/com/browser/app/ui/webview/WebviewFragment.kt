package com.browser.app.ui.webview

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.SafeBrowsingResponse
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.browser.app.WebviewFragmentArgs
import com.browser.app.databinding.FragmentWebviewBinding
import com.browser.app.utils.PreferenceManager
import com.browser.app.utils.SearchEngine
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class WebviewFragment : Fragment() {
    private var _binding: FragmentWebviewBinding? = null
    private val binding get() = _binding!!
    private val viewModel: WebviewViewModel by viewModels()
    @Inject lateinit var preferenceManager: PreferenceManager
    @Inject lateinit var noteRepository: com.browser.app.repository.NoteRepository

    private var currentUrl: String = ""
    private var currentTitle: String = ""
    private var isBookmarked = false
    /** 关联的多窗口 id；-1 表示本次浏览不绑定具体窗口 */
    private var windowId: Long = -1L

    /**
     * 系统返回键优先回退 WebView 历史，无历史时才退出 Fragment。
     * 修复之前系统返回键直接 popBack 退出整个 webview 的反直觉行为。
     */
    private val backCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            if (binding.webview.canGoBack()) {
                binding.webview.goBack()
            } else {
                isEnabled = false
                findNavController().navigateUp()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWebviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Safe Args 类型安全参数解析，替代手动 arguments?.getString/getLong
        val args = WebviewFragmentArgs.fromBundle(requireArguments())
        val url = args.url
        if (url.isBlank()) {
            findNavController().navigateUp()
            return
        }
        currentUrl = url
        windowId = args.windowId

        setupWebview()
        setupToolbar()
        setupBackCallback()
        loadUrl(url)
        observeBookmarkState()
    }

    private fun setupBackCallback() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backCallback)
        // 初始 webview 还没历史，不可用； onPageFinished 时根据 canGoBack 更新
        backCallback.isEnabled = false
    }

    private fun setupWebview() {
        binding.webview.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            builtInZoomControls = true
            displayZoomControls = false
            cacheMode = WebSettings.LOAD_DEFAULT
            // 远端采用最严格策略，禁止加载任何混合内容
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            // v1.9.0: 默认禁用文件访问，降低本地文件泄露风险
            allowFileAccess = false
            allowContentAccess = false
            // v1.9.0: 禁止通过 file:// 加载的页面访问其他源
            allowFileAccessFromFileURLs = false
            allowUniversalAccessFromFileURLs = false
            // v1.9.0: 启用 Safe Browsing（API 26+）
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                safeBrowsingEnabled = true
            }
        }

        // v1.9.0: 注入 JS Bridge，暴露 window.MSB 命名空间
        binding.webview.addJavascriptInterface(
            WebAppInterface(requireContext(), noteRepository),
            "MSB"
        )

        // v1.9.0: 启用 Cookie 持久化，并接受第三方 Cookie 以兼容登录态
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(binding.webview, true)
        }

        binding.webview.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                currentUrl = url ?: ""
                currentTitle = view?.title ?: ""
                binding.urlBar.setText(currentUrl)
                binding.progressBar.visibility = View.GONE
                binding.swipeRefresh.isRefreshing = false
                // 返回键回调可用性跟随 WebView 历史变化
                backCallback.isEnabled = binding.webview.canGoBack()

                // v1.9.0: 同步 Cookie 到持久化存储
                CookieManager.getInstance().flush()

                if (currentUrl.isNotBlank()) {
                    viewModel.addHistory(currentTitle, currentUrl)
                    // 多窗口真实化：浏览过程中回写当前窗口的 url+title
                    if (windowId >= 0) {
                        viewModel.updateWindow(windowId, currentUrl, currentTitle)
                    }
                }
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                binding.progressBar.visibility = View.GONE
                binding.swipeRefresh.isRefreshing = false
            }

            // v1.9.0: Safe Browsing 拦截回调，遇到威胁时回退到安全策略
            override fun onSafeBrowsingHit(
                view: WebView?,
                request: WebResourceRequest?,
                threatType: Int,
                callback: SafeBrowsingResponse?
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    callback?.backToSafety(true)
                } else {
                    super.onSafeBrowsingHit(view, request, threatType, callback)
                }
            }
        }

        binding.webview.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                binding.progressBar.apply {
                    progress = newProgress
                    visibility = if (newProgress in 1..99) View.VISIBLE else View.GONE
                }
            }

            override fun onJsAlert(
                view: WebView?,
                url: String?,
                message: String?,
                result: android.webkit.JsResult?
            ): Boolean {
                result?.confirm()
                return true
            }

            override fun onJsConfirm(
                view: WebView?,
                url: String?,
                message: String?,
                result: android.webkit.JsResult?
            ): Boolean {
                result?.cancel()
                return true
            }
        }

        binding.webview.setDownloadListener { url, _, _, _, _ ->
            // 交给系统浏览器处理下载
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                startActivity(intent)
            } catch (_: Exception) {
                // 无可用浏览器时静默忽略
            }
        }

        binding.swipeRefresh.setOnRefreshListener {
            binding.webview.reload()
        }
    }

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener {
            if (binding.webview.canGoBack()) {
                binding.webview.goBack()
            } else {
                findNavController().navigateUp()
            }
        }

        binding.urlBar.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_GO) {
                val input = binding.urlBar.text.toString().trim()
                if (input.isNotEmpty()) {
                    loadUrl(normalizeInput(input))
                }
                true
            } else {
                false
            }
        }

        binding.btnRefresh.setOnClickListener {
            binding.webview.reload()
        }

        binding.btnBookmark.setOnClickListener {
            toggleBookmark()
        }
    }

    /**
     * 区分 URL 与搜索词：输入像域名则补 https://，否则按当前搜索引擎搜索。
     * 修复之前输入 "abc" 直接拼成 https://abc 的体验缺陷。
     */
    private fun normalizeInput(input: String): String {
        if (input.startsWith("http://") || input.startsWith("https://")) {
            return input
        }
        // 简单识别域名：含 . 且无空格
        val looksLikeDomain = input.contains(".") && !input.contains(" ")
        return if (looksLikeDomain) {
            "https://$input"
        } else {
            val engine = SearchEngine.getById(preferenceManager.selectedSearchEngine)
            engine.searchUrl + java.net.URLEncoder.encode(input, "UTF-8")
        }
    }

    /**
     * 使用 repeatOnLifecycle 替代裸 launch+collect，避免每次 url 变化新增订阅导致的 Flow 泄漏。
     */
    private fun observeBookmarkState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isBookmarked(currentUrl).collect { bookmarked ->
                    isBookmarked = bookmarked
                    updateBookmarkIcon()
                }
            }
        }
    }

    private fun loadUrl(url: String) {
        if (url.isBlank()) return
        binding.progressBar.visibility = View.VISIBLE
        binding.webview.loadUrl(url)
        binding.urlBar.setText(url)
    }

    private fun toggleBookmark() {
        lifecycleScope.launch {
            val added = viewModel.toggleBookmark(currentTitle, currentUrl)
            isBookmarked = added
            updateBookmarkIcon()
            val msgRes = if (added) com.browser.app.R.string.bookmark_added
                else com.browser.app.R.string.bookmark_removed
            android.widget.Toast.makeText(requireContext(), msgRes, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateBookmarkIcon() {
        val iconRes = if (isBookmarked) {
            com.browser.app.R.drawable.ic_bookmark
        } else {
            com.browser.app.R.drawable.ic_bookmark_outline
        }
        binding.btnBookmark.setImageResource(iconRes)
        binding.btnBookmark.setColorFilter(
            if (isBookmarked) {
                requireContext().getColor(com.browser.app.R.color.accent)
            } else {
                requireContext().getColor(com.browser.app.R.color.gray)
            }
        )
    }

    override fun onPause() {
        super.onPause()
        binding.webview.onPause()
    }

    override fun onResume() {
        super.onResume()
        binding.webview.onResume()
    }

    override fun onDestroyView() {
        // v1.9.0: 移除 JS Bridge 引用，避免 Context 泄漏
        binding.webview.removeJavascriptInterface("MSB")
        binding.webview.destroy()
        super.onDestroyView()
        _binding = null
    }
}
