package com.browser.app.ui.webview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
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
        currentUrl = WebviewFragmentArgs.fromBundle(requireArguments()).url

        setupWebview()
        setupToolbar()
        observeBookmarkState()
        loadUrl(currentUrl)
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
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        }

        binding.webview.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                currentUrl = url ?: ""
                currentTitle = view?.title ?: ""
                binding.urlBar.setText(url)
                binding.progressBar.visibility = View.GONE
                binding.swipeRefresh.isRefreshing = false
                viewModel.onPageFinished(currentTitle, currentUrl)
            }
        }

        binding.webview.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                if (newProgress < 100) {
                    binding.progressBar.visibility = View.VISIBLE
                }
            }
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
        binding.btnBookmark.setOnClickListener { toggleBookmark() }
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

    override fun onDestroyView() {
        binding.webview.destroy()
        super.onDestroyView()
        _binding = null
    }
}
