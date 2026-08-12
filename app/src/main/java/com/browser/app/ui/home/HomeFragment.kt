package com.browser.app.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.browser.app.R
import com.browser.app.databinding.FragmentHomeBinding
import com.browser.app.utils.SearchEngine
import com.browser.app.utils.navigateToWebview
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HomeViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSearch()
        setupEngineGrid()
        observeSelectedEngine()
    }

    private fun setupEngineGrid() {
        val adapter = QuickLinkAdapter { engine ->
            viewModel.selectEngine(engine)
        }
        binding.quickLinks.layoutManager = GridLayoutManager(requireContext(), 4)
        binding.quickLinks.adapter = adapter
    }

    /**
     * 观察 ViewModel 的选中引擎状态，驱动 RecyclerView 高亮刷新。
     * 用 repeatOnLifecycle(STARTED) 避免在后台时收集造成资源浪费。
     */
    private fun observeSelectedEngine() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.selectedEngine.collect { engine ->
                    refreshEngineSelection(engine)
                }
            }
        }
    }

    private fun refreshEngineSelection(selected: SearchEngine) {
        val adapter = binding.quickLinks.adapter as? QuickLinkAdapter ?: return
        val items = SearchEngine.ALL.map { engine ->
            EngineItem(engine, engine.id == selected.id)
        }
        adapter.submitList(items)
    }

    private fun setupSearch() {
        binding.searchButton.setOnClickListener {
            performSearch()
        }

        binding.searchInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                performSearch()
                true
            } else {
                false
            }
        }
    }

    private fun performSearch() {
        val query = binding.searchInput.text.toString().trim()
        if (query.isNotEmpty()) {
            val selected = viewModel.selectedEngine.value
            val url = if (query.startsWith("http://") || query.startsWith("https://")) {
                query
            } else {
                selected.searchUrl + java.net.URLEncoder.encode(query, "UTF-8")
            }
            navigateToWebview(url)
        } else {
            Toast.makeText(requireContext(), R.string.input_search_query, Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToWebview(url: String) {
        findNavController().navigateToWebview(url)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
