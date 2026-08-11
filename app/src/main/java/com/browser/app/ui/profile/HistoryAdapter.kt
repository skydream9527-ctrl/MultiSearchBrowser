package com.browser.app.ui.profile

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.browser.app.R
import com.browser.app.data.entity.HistoryEntity
import com.browser.app.databinding.ItemHistoryBinding
import com.browser.app.databinding.ItemHistoryHeaderBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryAdapter(
    private val onItemClick: (HistoryEntity) -> Unit,
    private val onLongClick: (HistoryEntity) -> Unit
) : ListAdapter<HistoryListItem, RecyclerView.ViewHolder>(DIFF) {

    override fun getItemViewType(position: Int): Int =
        when (getItem(position)) {
            is HistoryListItem.Header -> TYPE_HEADER
            is HistoryListItem.Item -> TYPE_ITEM
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        when (viewType) {
            TYPE_HEADER -> HeaderViewHolder(
                ItemHistoryHeaderBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
            else -> ItemViewHolder(
                ItemHistoryBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is HistoryListItem.Header -> (holder as HeaderViewHolder).bind(item)
            is HistoryListItem.Item -> (holder as ItemViewHolder).bind(item.history)
        }
    }

    inner class HeaderViewHolder(private val binding: ItemHistoryHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(header: HistoryListItem.Header) {
            binding.title.text = header.label
        }
    }

    inner class ItemViewHolder(private val binding: ItemHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(history: HistoryEntity) {
            binding.title.text =
                history.title.ifEmpty { itemView.context.getString(R.string.text_no_title) }
            binding.url.text = history.url
            binding.timestamp.text = formatTimestamp(history.timestamp)
            binding.root.setOnClickListener { onItemClick(history) }
            binding.root.setOnLongClickListener {
                onLongClick(history)
                true
            }
        }

        /** 同一天内只显示时分，避免与分组标题重复 */
        private fun formatTimestamp(timestamp: Long): String {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }
    }

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ITEM = 1

        private val DIFF = object : DiffUtil.ItemCallback<HistoryListItem>() {
            override fun areItemsTheSame(
                oldItem: HistoryListItem,
                newItem: HistoryListItem
            ): Boolean = when {
                oldItem is HistoryListItem.Header && newItem is HistoryListItem.Header ->
                    oldItem.label == newItem.label
                oldItem is HistoryListItem.Item && newItem is HistoryListItem.Item ->
                    oldItem.history.id == newItem.history.id
                else -> false
            }

            override fun areContentsTheSame(
                oldItem: HistoryListItem,
                newItem: HistoryListItem
            ): Boolean = oldItem == newItem
        }
    }
}
