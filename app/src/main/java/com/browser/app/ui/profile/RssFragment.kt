package com.browser.app.ui.profile

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.browser.app.R
import com.browser.app.data.entity.RssItemEntity
import com.browser.app.databinding.FragmentRssBinding
import com.browser.app.databinding.ItemRssBinding
import com.browser.app.utils.navigateToWebview
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RssFragment : Fragment() {
    private var _binding: FragmentRssBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RssViewModel by viewModels()
    private lateinit var adapter: RssAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRssBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeItems()
        setupAddButton()
        setupBackButton()
    }

    private fun setupRecyclerView() {
        adapter = RssAdapter(
            onItemClick = { item ->
                findNavController().navigateToWebview(item.link)
            },
            onLongClick = { item ->
                showMarkAsReadDialog(item)
            }
        )
        binding.rssList.layoutManager = LinearLayoutManager(requireContext())
        binding.rssList.adapter = adapter
    }

    private fun observeItems() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.items.collect { items ->
                    adapter.submitList(items)
                    binding.emptyText.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
    }

    private fun setupAddButton() {
        binding.btnAdd.setOnClickListener {
            showAddFeedDialog()
        }
    }

    private fun setupBackButton() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun showAddFeedDialog() {
        val context = requireContext()
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 24)
        }
        val nameInput = EditText(context).apply {
            hint = "订阅源名称"
            inputType = InputType.TYPE_CLASS_TEXT
        }
        val urlInput = EditText(context).apply {
            hint = "https://example.com/rss"
            inputType = InputType.TYPE_TEXT_VARIATION_URI
        }
        container.addView(nameInput)
        container.addView(urlInput)
        AlertDialog.Builder(context)
            .setTitle(R.string.add_rss_feed)
            .setView(container)
            .setPositiveButton(R.string.action_clear) { _, _ ->
                val name = nameInput.text.toString().trim()
                val url = urlInput.text.toString().trim()
                if (name.isEmpty() || url.isEmpty()) {
                    Toast.makeText(context, R.string.input_search_query, Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.addFeed(name, url)
                }
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun showMarkAsReadDialog(item: RssItemEntity) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.mark_as_read)
            .setMessage(R.string.mark_as_read)
            .setPositiveButton(R.string.action_clear) { _, _ ->
                viewModel.markAsRead(item.guid)
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class RssAdapter(
    private val onItemClick: (RssItemEntity) -> Unit,
    private val onLongClick: (RssItemEntity) -> Unit
) : ListAdapter<RssItemEntity, RssAdapter.ViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRssBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemRssBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: RssItemEntity) {
            binding.title.text = item.title.ifEmpty { getString(R.string.no_title) }
            binding.source.text = item.source.ifEmpty { item.link }
            binding.pubDate.text = formatTimestamp(item.pubDate)
            binding.root.setOnClickListener { onItemClick(item) }
            binding.root.setOnLongClickListener {
                onLongClick(item)
                true
            }
        }

        private fun getString(resId: Int): String = itemView.context.getString(resId)

        private fun formatTimestamp(timestamp: Long): String {
            if (timestamp <= 0L) return ""
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
            return sdf.format(java.util.Date(timestamp))
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<RssItemEntity>() {
            override fun areItemsTheSame(oldItem: RssItemEntity, newItem: RssItemEntity) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: RssItemEntity, newItem: RssItemEntity) =
                oldItem == newItem
        }
    }
}
