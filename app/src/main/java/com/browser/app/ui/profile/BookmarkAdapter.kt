package com.browser.app.ui.profile

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.browser.app.R
import com.browser.app.data.entity.BookmarkEntity
import com.browser.app.databinding.ItemBookmarkBinding

class BookmarkAdapter(
    private val onItemClick: (BookmarkEntity) -> Unit,
    private val onLongClick: (BookmarkEntity) -> Unit
) : ListAdapter<BookmarkEntity, BookmarkAdapter.ViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemBookmarkBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemBookmarkBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(bookmark: BookmarkEntity) {
            binding.title.text = bookmark.title.ifEmpty { itemView.context.getString(R.string.text_no_title) }
            binding.url.text = if (bookmark.folder.isEmpty()) {
                bookmark.url
            } else {
                "📁 ${bookmark.folder} · ${bookmark.url}"
            }
            binding.root.setOnClickListener { onItemClick(bookmark) }
            binding.root.setOnLongClickListener {
                onLongClick(bookmark)
                true
            }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<BookmarkEntity>() {
            override fun areItemsTheSame(oldItem: BookmarkEntity, newItem: BookmarkEntity): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: BookmarkEntity, newItem: BookmarkEntity): Boolean =
                oldItem == newItem
        }
    }
}
