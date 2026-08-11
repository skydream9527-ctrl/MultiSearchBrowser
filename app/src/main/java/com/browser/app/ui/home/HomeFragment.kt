package com.browser.app.ui.home

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.GridLayoutManager
import com.browser.app.R
import com.browser.app.databinding.FragmentHomeBinding
import com.browser.app.utils.SearchEngine
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()
    private var selectedEngine: SearchEngine = SearchEngine.BAIDU
    private lateinit var suggestionAdapter: SuggestionAdapter
    private val searchInputWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable?) {
            viewModel.updateSuggestions(s?.toString().orEmpty())
        }
    }

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
        selectedEngine = viewModel.selectedEngine()
        setupSearchEngines()
        setupSearch()
        setupSuggestions()
        setupQuickLinks()
        setupCommonSites()
    }

    private fun setupQuickLinks() {
        val adapter = QuickLinkAdapter { engine ->
            selectEngine(engine)
        }
        binding.quickLinks.layoutManager = GridLayoutManager(requireContext(), 4)
        binding.quickLinks.adapter = adapter
    }

    private fun setupCommonSites() {
        val adapter = QuickSiteAdapter { site ->
            navigateToWebview(site.url)
        }
        binding.commonSites.layoutManager = GridLayoutManager(requireContext(), 4)
        binding.commonSites.adapter = adapter
    }

    private fun setupSearchEngines() {
        SearchEngine.ALL.forEach { engine ->
            val chip = Chip(requireContext()).apply {
                text = getString(engine.nameResId)
                isCheckable = true
                tag = engine.id
                setChipBackgroundColorResource(com.browser.app.R.color.white)
                if (engine.id == selectedEngine.id) {
                    isChecked = true
                    setChipStrokeColorResource(com.browser.app.R.color.primary)
                    chipStrokeWidth = 2f
                }
                setOnClickListener { selectEngine(engine) }
            }
            binding.engineChips.addView(chip)
        }
    }

    private fun selectEngine(engine: SearchEngine) {
        selectedEngine = engine
        viewModel.selectEngine(engine)
        updateChipStyles()
    }

    private fun updateChipStyles() {
        for (i in 0 until binding.engineChips.childCount) {
            val chip = binding.engineChips.getChildAt(i) as Chip
            if (chip.tag == selectedEngine.id) {
                chip.chipStrokeWidth = 2f
                chip.setChipStrokeColorResource(com.browser.app.R.color.primary)
            } else {
                chip.chipStrokeWidth = 0f
            }
        }
    }

    private fun setupSearch() {
        binding.searchButton.setOnClickListener { performSearch() }

        binding.searchInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                performSearch()
                true
            } else {
                false
            }
        }

        // 实时输入触发建议
        binding.searchInput.addTextChangedListener(searchInputWatcher)

        // 失去焦点时隐藏下拉
        binding.searchInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                binding.suggestionsList.isVisible = false
            }
        }
    }

    private fun setupSuggestions() {
        suggestionAdapter = SuggestionAdapter { suggestion ->
            binding.suggestionsList.isVisible = false
            val url = when (suggestion) {
                is HomeViewModel.SearchSuggestion.DirectSearch -> {
                    viewModel.buildSearchUrl(suggestion.query)
                }
                is HomeViewModel.SearchSuggestion.History -> suggestion.url
            }
            if (url != null) {
                // 把选中的关键词填回输入框，便于用户继续编辑
                binding.searchInput.setText(
                    when (suggestion) {
                        is HomeViewModel.SearchSuggestion.DirectSearch -> suggestion.query
                        is HomeViewModel.SearchSuggestion.History -> suggestion.displayText
                    }
                )
                navigateToWebview(url)
            } else {
                Toast.makeText(requireContext(), R.string.search_empty_input, Toast.LENGTH_SHORT).show()
            }
        }
        binding.suggestionsList.layoutManager = LinearLayoutManager(requireContext())
        binding.suggestionsList.adapter = suggestionAdapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.suggestions.collect { list ->
                    suggestionAdapter.submitList(list) {
                        // 显示 / 隐藏下拉：有内容且 search_input 持有焦点
                        binding.suggestionsList.isVisible =
                            list.isNotEmpty() && binding.searchInput.hasFocus()
                    }
                }
            }
        }
    }

    private fun performSearch() {
        val raw = binding.searchInput.text.toString().trim()
        val url = viewModel.buildSearchUrl(raw)
        if (url != null) {
            // 触发搜索时隐藏下拉
            binding.suggestionsList.isVisible = false
            navigateToWebview(url)
        } else {
            Toast.makeText(requireContext(), R.string.search_empty_input, Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToWebview(url: String) {
        val action = HomeFragmentDirections.actionHomeFragmentToWebviewFragment(url)
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        // 解绑 TextWatcher 避免持有已 destroy 的 binding
        binding.searchInput.removeTextChangedListener(searchInputWatcher)
        super.onDestroyView()
        _binding = null
    }
}
