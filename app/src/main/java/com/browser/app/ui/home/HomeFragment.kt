package com.browser.app.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.browser.app.R
import com.browser.app.databinding.FragmentHomeBinding
import com.browser.app.utils.SearchEngine
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()
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
        selectedEngine = viewModel.selectedEngine()
        setupSearchEngines()
        setupSearch()
        setupQuickLinks()
    }

    private fun setupQuickLinks() {
        val adapter = QuickLinkAdapter { engine ->
            selectEngine(engine)
        }
        binding.quickLinks.layoutManager = GridLayoutManager(requireContext(), 4)
        binding.quickLinks.adapter = adapter
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
    }

    private fun performSearch() {
        val raw = binding.searchInput.text.toString().trim()
        val url = viewModel.buildSearchUrl(raw)
        if (url != null) {
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
        super.onDestroyView()
        _binding = null
    }
}
