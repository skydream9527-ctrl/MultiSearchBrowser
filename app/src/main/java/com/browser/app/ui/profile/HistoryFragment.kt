package com.browser.app.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.browser.app.R
import com.browser.app.data.BrowserDatabase
import com.browser.app.data.entity.HistoryEntity
import com.browser.app.databinding.FragmentHistoryBinding
import com.browser.app.databinding.ItemHistoryBinding
import com.browser.app.repository.HistoryRepository
import com.browser.app.utils.navigateToWebview
import kotlinx.coroutines.launch

class HistoryFragment : Fragment() {
    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!
    private lateinit var historyRepository: HistoryRepository
    private lateinit var adapter: HistoryAdapter

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
        val db = BrowserDatabase.getInstance(requireContext())
        historyRepository = HistoryRepository(db.historyDao())

        setupRecyclerView()
        observeHistory()
        setupClearButton()
    }

    private fun setupRecyclerView() {
        adapter = HistoryAdapter(
            onItemClick = { history ->
                findNavController().navigateToWebview(history.url)
            },
            onLongClick = { history ->
                showDeleteDialog(history)
            }
        )
        binding.historyList.layoutManager = LinearLayoutManager(requireContext())
        binding.historyList.adapter = adapter
    }

    private fun observeHistory() {
        lifecycleScope.launch {
            historyRepository.getAllHistory().collect { historyList ->
                adapter.submitList(historyList)
                binding.emptyText.visibility = if (historyList.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun setupClearButton() {
        binding.btnClear.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.clear_history)
                .setMessage(R.string.clear_history_confirm)
                .setPositiveButton(R.string.action_clear) { _, _ ->
                    lifecycleScope.launch {
                        historyRepository.clearHistory()
                    }
                }
                .setNegativeButton(R.string.action_cancel, null)
                .show()
        }
    }

    private fun showDeleteDialog(history: HistoryEntity) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete_record)
            .setMessage(R.string.confirm_delete_record)
            .setPositiveButton(R.string.action_delete) { _, _ ->
                lifecycleScope.launch {
                    historyRepository.deleteHistory(history)
                }
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class HistoryAdapter(
    private val onItemClick: (HistoryEntity) -> Unit,
    private val onLongClick: (HistoryEntity) -> Unit
) : ListAdapter<HistoryEntity, HistoryAdapter.ViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(history: HistoryEntity) {
            binding.title.text = history.title.ifEmpty { getString(R.string.no_title) }
            binding.url.text = history.url
            binding.timestamp.text = formatTimestamp(history.timestamp)
            binding.root.setOnClickListener { onItemClick(history) }
            binding.root.setOnLongClickListener {
                onLongClick(history)
                true
            }
        }

        private fun formatTimestamp(timestamp: Long): String {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
            return sdf.format(java.util.Date(timestamp))
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<HistoryEntity>() {
            override fun areItemsTheSame(oldItem: HistoryEntity, newItem: HistoryEntity) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: HistoryEntity, newItem: HistoryEntity) = oldItem == newItem
        }
    }
}