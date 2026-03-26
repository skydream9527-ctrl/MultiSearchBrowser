package com.browser.app.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.browser.app.R
import com.browser.app.databinding.ItemQuickLinkBinding
import com.browser.app.utils.SearchEngine

class QuickLinkAdapter(
    private val onItemClick: (SearchEngine) -> Unit
) : RecyclerView.Adapter<QuickLinkAdapter.ViewHolder>() {

    private val items = SearchEngine.ALL

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemQuickLinkBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class ViewHolder(private val binding: ItemQuickLinkBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(engine: SearchEngine) {
            binding.name.text = engine.name
            binding.root.setOnClickListener { onItemClick(engine) }
        }
    }
}