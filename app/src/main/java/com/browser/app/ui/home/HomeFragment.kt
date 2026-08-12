package com.browser.app.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.browser.app.databinding.FragmentHomeBinding
import com.browser.app.utils.PreferenceManager
import com.browser.app.utils.SearchEngine
import com.browser.app.utils.navigateToWebview

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var preferenceManager: PreferenceManager
    private var selectedEngine: SearchEngine = SearchEngine.BAIDU

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
        preferenceManager = PreferenceManager(requireContext())
        setupSearchEngines()
        setupSearch()
        setupQuickLinks()
    }

    private fun setupQuickLinks() {
        val adapter = QuickLinkAdapter { engine ->
            selectedEngine = engine
            preferenceManager.selectedSearchEngine = engine.id
            updateChipStyles()
        }
        binding.quickLinks.layoutManager = GridLayoutManager(requireContext(), 4)
        binding.quickLinks.adapter = adapter
    }

    private fun setupSearchEngines() {
        val engineId = preferenceManager.selectedSearchEngine
        selectedEngine = SearchEngine.getById(engineId)

        SearchEngine.ALL.forEach { engine ->
            val chip = com.google.android.material.chip.Chip(requireContext()).apply {
                text = engine.name
                isCheckable = true
                tag = engine.id
                setChipBackgroundColorResource(com.browser.app.R.color.white)
                if (engine.id == selectedEngine.id) {
                    isChecked = true
                    setChipStrokeColorResource(com.browser.app.R.color.primary)
                    chipStrokeWidth = 2f
                }
                setOnClickListener {
                    selectedEngine = engine
                    preferenceManager.selectedSearchEngine = engine.id
                    updateChipStyles()
                }
            }
            binding.engineChips.addView(chip)
        }
    }

    private fun updateChipStyles() {
        for (i in 0 until binding.engineChips.childCount) {
            val chip = binding.engineChips.getChildAt(i) as com.google.android.material.chip.Chip
            if (chip.tag == selectedEngine.id) {
                chip.chipStrokeWidth = 2f
                chip.setChipStrokeColorResource(com.browser.app.R.color.primary)
            } else {
                chip.chipStrokeWidth = 0f
            }
        }
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
            val url = if (query.startsWith("http://") || query.startsWith("https://")) {
                query
            } else {
                selectedEngine.searchUrl + java.net.URLEncoder.encode(query, "UTF-8")
            }
            navigateToWebview(url)
        } else {
            Toast.makeText(requireContext(), "请输入搜索内容", Toast.LENGTH_SHORT).show()
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