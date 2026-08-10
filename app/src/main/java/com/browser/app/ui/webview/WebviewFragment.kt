package com.browser.app.ui.webview

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.browser.app.R
import com.browser.app.databinding.FragmentWebviewBinding
import com.browser.app.webview.WebViewPool
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class WebviewFragment : Fragment() {
    private var _binding: FragmentWebviewBinding? = null
    private val binding get() = _binding!!
    private val viewModel: WebviewViewModel by viewModels()

    @javax.inject.Inject
    lateinit var webViewPool: WebViewPool

    @javax.inject.Inject
    lateinit var preferenceManager: com.browser.app.utils.PreferenceManager

    @javax.inject.Inject
    lateinit var downloadRepository: com.browser.app.repository.DownloadRepository

    /** 由 WebViewPool 复用提供的 WebView，Fragment 销毁时不 destroy，归还给池。 */
    private lateinit var webView: WebView

    private var currentUrl: String = ""
    private var currentTitle: String = ""
    private var isBookmarked = false
    private var useDesktopUA = false

    /** 文件选择回调（onShowFileChooser 触发） */
    private var pendingFileChooserCallback: android.webkit.ValueCallback<Array<Uri>>? = null

    private val fileChooserLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val callback = pendingFileChooserCallback
        pendingFileChooserCallback = null
        val uris: Array<Uri>? = if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data = result.data
            val clipData = data?.clipData
            val dataString = data?.dataString
            when {
                clipData != null -> Array(clipData.itemCount) { clipData.getItemAt(it).uri }
                dataString != null -> arrayOf(Uri.parse(dataString))
                else -> null
            }
        } else {
            null
        }
        callback?.onReceiveValue(uris)
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
        val args = WebviewFragmentArgs.fromBundle(requireArguments())
        val incomingUrl = args.url
        val windowId = args.windowId

        attachWebView(windowId)
        setupToolbar()
        setupErrorRetry()

        // 多窗口：windowId > 0 时从 DB 加载已存在的 tab；否则新建一个 tab。
        // 注意：windowId > 0 时 WebView 复用 pool，若之前未加载过 url（首次进入），仍需 loadUrl。
        viewLifecycleOwner.lifecycleScope.launch {
            val realUrl = viewModel.initTab(windowId, incomingUrl)
            currentUrl = realUrl
            observeBookmarkState()
            // 仅当 WebView 当前 url 与目标 url 不同时才加载，避免切 tab 重复刷新
            if (webView.url == null || webView.url != realUrl) {
                loadUrl(realUrl)
            } else {
                // 复用同一 WebView：URL 没变，仅同步 UI 状态
                binding.urlBar.setText(realUrl)
                updateNavigationButtons()
            }
        }
    }

    /**
     * 从 WebViewPool 拿到 windowId 对应的 WebView，挂载到容器里。
     * 若 WebView 已经在父容器里（从其他 Fragment 切过来），先 detach 再 attach。
     */
    private fun attachWebView(windowId: Long) {
        webView = webViewPool.obtain(windowId)
        (webView.parent as? ViewGroup)?.removeView(webView)
        binding.webviewContainer.addView(
            webView,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        // 用户在设置里选了"桌面 UA"则初始化 useDesktopUA = true，
        // 单次长按 refresh 仍可临时切换，但切 tab 后会回到设置默认值
        useDesktopUA = preferenceManager.defaultUserAgent == "desktop"
        webView.settings.userAgentString = if (useDesktopUA) DESKTOP_UA else null
        setupWebview()
    }

    private fun setupWebview() {
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val url = request?.url?.toString() ?: return false
                val scheme = request.url.scheme?.lowercase() ?: return false
                // 非 http(s) 链接交给系统处理（tel/mailto/weixin 等 App 跳转）
                if (scheme != "http" && scheme != "https") {
                    return handleExternalUrl(url)
                }
                return false
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                currentUrl = url ?: ""
                currentTitle = view?.title ?: ""
                binding.urlBar.setText(url)
                binding.swipeRefresh.isRefreshing = false
                hideError()
                viewModel.onPageFinished(currentTitle, currentUrl)
                updateNavigationButtons()
                // 同步 Cookie
                CookieManager.getInstance().flush()
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: android.webkit.WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                // 只对主资源错误显示错误页，子资源失败忽略
                if (request?.isForMainFrame == true) {
                    showError(getString(R.string.webview_error_network))
                }
            }

            override fun onReceivedHttpError(
                view: WebView?,
                request: WebResourceRequest?,
                errorResponse: WebResourceResponse?
            ) {
                super.onReceivedHttpError(view, request, errorResponse)
                if (request?.isForMainFrame == true) {
                    val code = errorResponse?.statusCode ?: 0
                    showError(getString(R.string.webview_error_http, code))
                }
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                if (newProgress < 100) {
                    binding.progressBar.visibility = View.VISIBLE
                } else {
                    binding.progressBar.visibility = View.GONE
                }
            }

            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: android.webkit.ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                // 取消上一个未完成的回调，避免泄漏
                pendingFileChooserCallback?.onReceiveValue(null)
                pendingFileChooserCallback = filePathCallback
                val intent = fileChooserParams?.createIntent() ?: run {
                    pendingFileChooserCallback = null
                    return false
                }
                return try {
                    fileChooserLauncher.launch(intent)
                    true
                } catch (e: Exception) {
                    pendingFileChooserCallback = null
                    Toast.makeText(
                        requireContext(),
                        R.string.webview_filechooser_failed,
                        Toast.LENGTH_SHORT
                    ).show()
                    false
                }
            }
        }

        // 下载：交给系统 DownloadManager
        webView.setDownloadListener { url, _, _, mimetype, _ ->
            handleDownload(url, mimetype)
        }

        binding.swipeRefresh.setOnRefreshListener { webView.reload() }
    }

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener {
            if (webView.canGoBack()) {
                webView.goBack()
            } else {
                findNavController().navigateUp()
            }
        }

        binding.btnForward.setOnClickListener {
            if (webView.canGoForward()) {
                webView.goForward()
            }
        }

        binding.urlBar.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_GO) {
                val url = binding.urlBar.text.toString().trim()
                if (url.isNotEmpty()) {
                    val finalUrl = if (url.startsWith("http://") || url.startsWith("https://")) {
                        url
                    } else {
                        "https://$url"
                    }
                    loadUrl(finalUrl)
                }
                true
            } else {
                false
            }
        }

        binding.btnRefresh.setOnClickListener { webView.reload() }
        // 长按 refresh 切换桌面/移动 UA
        binding.btnRefresh.setOnLongClickListener {
            toggleUserAgent()
            true
        }

        binding.btnBookmark.setOnClickListener { toggleBookmark() }
    }

    private fun setupErrorRetry() {
        binding.btnRetry.setOnClickListener {
            hideError()
            webView.reload()
        }
    }

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
        binding.progressBar.visibility = View.VISIBLE
        webView.loadUrl(url)
        binding.urlBar.setText(url)
    }

    private fun toggleBookmark() {
        viewModel.toggleBookmark(currentTitle, currentUrl) { added ->
            isBookmarked = added
            updateBookmarkIcon()
            val msgRes = if (added) R.string.bookmark_added else R.string.bookmark_removed
            Toast.makeText(requireContext(), msgRes, Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateBookmarkIcon() {
        val iconRes = if (isBookmarked) R.drawable.ic_bookmark else R.drawable.ic_bookmark_outline
        binding.btnBookmark.setImageResource(iconRes)
        binding.btnBookmark.setColorFilter(
            if (isBookmarked) {
                requireContext().getColor(R.color.accent)
            } else {
                requireContext().getColor(R.color.gray)
            }
        )
    }

    private fun updateNavigationButtons() {
        binding.btnBack.isEnabled = webView.canGoBack()
        binding.btnForward.isEnabled = webView.canGoForward()
        binding.btnBack.alpha = if (binding.btnBack.isEnabled) 1f else 0.4f
        binding.btnForward.alpha = if (binding.btnForward.isEnabled) 1f else 0.4f
    }

    private fun toggleUserAgent() {
        useDesktopUA = !useDesktopUA
        val ua = if (useDesktopUA) DESKTOP_UA else null
        webView.settings.userAgentString = ua
        val label = if (useDesktopUA) {
            getString(R.string.webview_ua_desktop)
        } else {
            getString(R.string.webview_ua_mobile)
        }
        Toast.makeText(
            requireContext(),
            getString(R.string.webview_ua_switched, label),
            Toast.LENGTH_SHORT
        ).show()
        webView.reload()
    }

    private fun handleExternalUrl(url: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (intent.resolveActivity(requireContext().packageManager) != null) {
                startActivity(intent)
            } else {
                Toast.makeText(
                    requireContext(),
                    R.string.webview_no_app_to_open,
                    Toast.LENGTH_SHORT
                ).show()
            }
            true
        } catch (e: Exception) {
            Toast.makeText(
                requireContext(),
                R.string.webview_no_app_to_open,
                Toast.LENGTH_SHORT
            ).show()
            true
        }
    }

    private fun handleDownload(url: String, mimetype: String?) {
        try {
            val request = DownloadManager.Request(Uri.parse(url)).apply {
                setMimeType(mimetype ?: "*/*")
                setNotificationVisibility(
                    DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                )
                setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS,
                    Uri.parse(url).lastPathSegment ?: "download"
                )
                // 携带当前 WebView 的 Cookie
                val cookies = CookieManager.getInstance().getCookie(url)
                if (!cookies.isNullOrEmpty()) {
                    addRequestHeader("Cookie", cookies)
                }
                // 状态栏通知里显示文件名 / 下载来源
                setTitle(Uri.parse(url).lastPathSegment ?: url)
            }
            val dm = requireContext()
                .getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val downloadId = dm.enqueue(request)
            // 落库一条下载记录，便于 DownloadsFragment 展示
            viewLifecycleOwner.lifecycleScope.launch {
                downloadRepository.insertRecord(
                    downloadId = downloadId,
                    title = Uri.parse(url).lastPathSegment ?: url,
                    url = url,
                    mimetype = mimetype
                )
            }
            Toast.makeText(
                requireContext(),
                R.string.webview_download_started,
                Toast.LENGTH_SHORT
            ).show()
        } catch (e: Exception) {
            // 回退：交给系统浏览器处理
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            } catch (_: Exception) {
                Toast.makeText(
                    requireContext(),
                    R.string.webview_download_failed,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun showError(message: String) {
        binding.errorLayout.visibility = View.VISIBLE
        binding.errorMessage.text = message
        binding.progressBar.visibility = View.GONE
        binding.swipeRefresh.isRefreshing = false
    }

    private fun hideError() {
        binding.errorLayout.visibility = View.GONE
    }

    override fun onPause() {
        webView.onPause()
        webView.pauseTimers()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
        webView.resumeTimers()
        updateNavigationButtons()
    }

    override fun onDestroyView() {
        // 取消未完成的文件选择回调，防止 WebView 卡死
        pendingFileChooserCallback?.onReceiveValue(null)
        pendingFileChooserCallback = null
        // 解绑客户端回调：避免切回时回调持有已 destroy 的 binding
        webView.setDownloadListener(null)
        // 从容器移除 WebView，但不 destroy —— 它由 WebViewPool 统一管理生命周期
        (webView.parent as? ViewGroup)?.removeView(webView)
        binding.swipeRefresh.setOnRefreshListener(null)
        super.onDestroyView()
        _binding = null
    }

    companion object {
        // Chrome 桌面版 UA
        private const val DESKTOP_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    }
}
