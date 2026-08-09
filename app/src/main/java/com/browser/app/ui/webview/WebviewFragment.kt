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
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
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
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class WebviewFragment : Fragment() {
    private var _binding: FragmentWebviewBinding? = null
    private val binding get() = _binding!!
    private val viewModel: WebviewViewModel by viewModels()

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

        setupWebview()
        setupToolbar()
        setupErrorRetry()

        // 多窗口：windowId > 0 时从 DB 加载已存在的 tab；否则新建一个 tab
        viewLifecycleOwner.lifecycleScope.launch {
            val realUrl = viewModel.initTab(windowId, incomingUrl)
            currentUrl = realUrl
            observeBookmarkState()
            loadUrl(realUrl)
        }
    }

    private fun setupWebview() {
        binding.webview.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            builtInZoomControls = true
            displayZoomControls = false
            cacheMode = WebSettings.LOAD_DEFAULT
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            // 允许文件上传
            allowFileAccess = false
            allowContentAccess = true
            mediaPlaybackRequiresUserGesture = true
        }

        // 启用 Cookie 持久化
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(binding.webview, false)

        binding.webview.webViewClient = object : WebViewClient() {
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
                errorResponse: android.webkit.WebResourceResponse?
            ) {
                super.onReceivedHttpError(view, request, errorResponse)
                if (request?.isForMainFrame == true) {
                    val code = errorResponse?.statusCode ?: 0
                    showError(getString(R.string.webview_error_http, code))
                }
            }
        }

        binding.webview.webChromeClient = object : WebChromeClient() {
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
        binding.webview.setDownloadListener { url, _, _, mimetype, _ ->
            handleDownload(url, mimetype)
        }

        binding.swipeRefresh.setOnRefreshListener { binding.webview.reload() }
    }

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener {
            if (binding.webview.canGoBack()) {
                binding.webview.goBack()
            } else {
                findNavController().navigateUp()
            }
        }

        binding.btnForward.setOnClickListener {
            if (binding.webview.canGoForward()) {
                binding.webview.goForward()
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

        binding.btnRefresh.setOnClickListener { binding.webview.reload() }
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
            binding.webview.reload()
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
        binding.webview.loadUrl(url)
        binding.urlBar.setText(url)
    }

    private fun toggleBookmark() {
        viewModel.toggleBookmark(currentTitle, currentUrl) { added ->
            isBookmarked = added
            updateBookmarkIcon()
            val message = if (added) "已添加收藏" else "已取消收藏"
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
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
        binding.btnBack.isEnabled = binding.webview.canGoBack()
        binding.btnForward.isEnabled = binding.webview.canGoForward()
        binding.btnBack.alpha = if (binding.btnBack.isEnabled) 1f else 0.4f
        binding.btnForward.alpha = if (binding.btnForward.isEnabled) 1f else 0.4f
    }

    private fun toggleUserAgent() {
        useDesktopUA = !useDesktopUA
        val ua = if (useDesktopUA) DESKTOP_UA else null
        binding.webview.settings.userAgentString = ua
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
        binding.webview.reload()
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
            }
            val dm = requireContext()
                .getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            dm.enqueue(request)
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
        binding.webview.onPause()
        binding.webview.pauseTimers()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        binding.webview.onResume()
        binding.webview.resumeTimers()
        updateNavigationButtons()
    }

    override fun onDestroyView() {
        // 取消未完成的文件选择回调，防止 WebView 卡死
        pendingFileChooserCallback?.onReceiveValue(null)
        pendingFileChooserCallback = null
        binding.webview.destroy()
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
