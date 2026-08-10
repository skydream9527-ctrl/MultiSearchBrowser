package com.browser.app.ui.tabs

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.browser.app.R
import com.browser.app.data.entity.WindowEntity
import com.browser.app.databinding.ItemWindowBinding

class WindowAdapter(
    private val onItemClick: (WindowEntity) -> Unit,
    private val onCloseClick: (WindowEntity) -> Unit
) : ListAdapter<WindowEntity, WindowAdapter.ViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemWindowBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemWindowBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(window: WindowEntity) {
            binding.title.text = window.title.ifEmpty { itemView.context.getString(R.string.text_no_title) }
            binding.url.text = window.url
            binding.root.setOnClickListener { onItemClick(window) }
            binding.closeBtn.setOnClickListener { onCloseClick(window) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<WindowEntity>() {
            override fun areItemsTheSame(oldItem: WindowEntity, newItem: WindowEntity): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: WindowEntity, newItem: WindowEntity): Boolean =
                oldItem == newItem
        }
    }
}
