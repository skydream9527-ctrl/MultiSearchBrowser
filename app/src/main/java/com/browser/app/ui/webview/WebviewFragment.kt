package com.browser.app.ui.webview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.browser.app.data.BrowserDatabase
import com.browser.app.databinding.FragmentWebviewBinding
import com.browser.app.repository.BookmarkRepository
import com.browser.app.repository.HistoryRepository
import com.browser.app.repository.WindowRepository
import com.browser.app.utils.PreferenceManager
import com.browser.app.utils.SearchEngine
import kotlinx.coroutines.launch

class WebviewFragment : Fragment() {
    private var _binding: FragmentWebviewBinding? = null
    private val binding get() = _binding!!
    private lateinit var historyRepository: HistoryRepository
    private lateinit var bookmarkRepository: BookmarkRepository
    private lateinit var windowRepository: WindowRepository
    private lateinit var preferenceManager: PreferenceManager
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
        val db = BrowserDatabase.getInstance(requireContext())
        historyRepository = HistoryRepository(db.historyDao())
        bookmarkRepository = BookmarkRepository(db.bookmarkDao())
        windowRepository = WindowRepository(db.windowDao())
        preferenceManager = PreferenceManager(requireContext())

        // 修复空 URL 加载导致的崩溃：空时回退到首页
        val url = arguments?.getString("url") ?: ""
        if (url.isBlank()) {
            findNavController().navigateUp()
            return
        }
        currentUrl = url
        windowId = arguments?.getLong("windowId", -1L) ?: -1L

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

                if (currentUrl.isNotBlank()) {
                    historyRepository.addHistory(currentTitle, currentUrl)
                    // 多窗口真实化：浏览过程中回写当前窗口的 url+title
                    if (windowId >= 0) {
                        lifecycleScope.launch {
                            val existing = windowRepository.getWindowById(windowId)
                            if (existing != null) {
                                windowRepository.updateWindow(
                                    existing.copy(url = currentUrl, title = currentTitle, timestamp = System.currentTimeMillis())
                                )
                            }
                        }
                    }
                }
            }

            override fun onReceivedError(
                view: WebView?,
                request: android.webkit.WebResourceRequest?,
                error: android.webkit.WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                binding.progressBar.visibility = View.GONE
                binding.swipeRefresh.isRefreshing = false
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
                bookmarkRepository.isBookmarked(currentUrl).collect { bookmarked ->
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
            val added = bookmarkRepository.toggleBookmark(currentTitle, currentUrl)
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
        binding.webview.destroy()
        super.onDestroyView()
        _binding = null
    }
}
