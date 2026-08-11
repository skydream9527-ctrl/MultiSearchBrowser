package com.browser.app.ui.profile

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.browser.app.R
import com.browser.app.data.entity.HistoryEntity
import com.browser.app.databinding.FragmentHistoryBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HistoryFragment : Fragment() {
    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HistoryViewModel by viewModels()
    private lateinit var adapter: HistoryAdapter

    private val searchInputWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            viewModel.updateSearchQuery(s?.toString().orEmpty())
        }
        override fun afterTextChanged(s: Editable?) = Unit
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupSearch()
        observeItems()
    }

    private fun setupRecyclerView() {
        adapter = HistoryAdapter(
            onItemClick = { history ->
                val action = HistoryFragmentDirections
                    .actionHistoryFragmentToWebviewFragment(history.url)
                findNavController().navigate(action)
            },
            onLongClick = { history -> showDeleteDialog(history) }
        )
        binding.historyList.layoutManager = LinearLayoutManager(requireContext())
        binding.historyList.adapter = adapter
    }

    private fun setupSearch() {
        binding.searchInput.addTextChangedListener(searchInputWatcher)
    }

    private fun observeItems() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.items.collect { items ->
                    adapter.submitList(items)
                    binding.emptyText.visibility =
                        if (items.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
    }

    private fun showDeleteDialog(history: HistoryEntity) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.dialog_delete_history_title)
            .setMessage(R.string.dialog_delete_history_message)
            .setPositiveButton(R.string.dialog_delete) { _, _ -> viewModel.deleteHistory(history) }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.searchInput.removeTextChangedListener(searchInputWatcher)
        _binding = null
    }
}
