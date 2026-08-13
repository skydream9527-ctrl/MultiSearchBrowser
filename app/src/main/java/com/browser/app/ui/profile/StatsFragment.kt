package com.browser.app.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.browser.app.R
import com.browser.app.databinding.FragmentStatsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class StatsFragment : Fragment() {
    private var _binding: FragmentStatsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: StatsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupBackButton()
        observeStats()
    }

    private fun setupBackButton() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun observeStats() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.historyCount.text = formatCount(R.string.stats_history_count, state.historyCount)
                    binding.bookmarkCount.text = formatCount(R.string.stats_bookmark_count, state.bookmarkCount)
                    binding.windowCount.text = formatCount(R.string.stats_window_count, state.windowCount)
                    binding.noteCount.text = formatCount(R.string.stats_note_count, state.noteCount)
                    binding.rssCount.text = formatCount(R.string.stats_rss_count, state.rssCount)
                    binding.topSites.text = formatTopSites(state.topSites)
                }
            }
        }
    }

    private fun formatCount(labelRes: Int, count: Int): String {
        return "${getString(labelRes)}: $count"
    }

    private fun formatTopSites(topSites: List<Pair<String, Int>>): String {
        val builder = StringBuilder()
        builder.append(getString(R.string.stats_top_sites)).append("\n\n")
        if (topSites.isEmpty()) {
            builder.append(getString(R.string.no_history))
        } else {
            topSites.forEachIndexed { index, (host, count) ->
                builder.append("${index + 1}. $host  ($count)\n")
            }
        }
        return builder.toString().trimEnd()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
