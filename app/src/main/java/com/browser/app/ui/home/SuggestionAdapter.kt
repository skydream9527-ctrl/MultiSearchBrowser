package com.browser.app.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.browser.app.R
import com.browser.app.databinding.ItemSuggestionBinding

/**
 * 搜索建议列表适配器：
 * - DirectSearch：显示放大镜 + "用 {引擎} 搜索 {query}"
 * - History：显示历史小图标 + 命中页面的 title / url
 */
class SuggestionAdapter(
    private val onClick: (HomeViewModel.SearchSuggestion) -> Unit
) : ListAdapter<HomeViewModel.SearchSuggestion, SuggestionAdapter.VH>(DIFF) {

    override fun getItemViewType(position: Int): Int =
        if (getItem(position) is HomeViewModel.SearchSuggestion.DirectSearch) TYPE_DIRECT else TYPE_HISTORY

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemSuggestionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    inner class VH(
        private val binding: ItemSuggestionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: HomeViewModel.SearchSuggestion) {
            binding.suggestionText.text = item.displayText
            binding.suggestionIcon.setImageResource(
                if (item is HomeViewModel.SearchSuggestion.DirectSearch) {
                    R.drawable.ic_search
                } else {
                    R.drawable.ic_history
                }
            )
            binding.root.setOnClickListener { onClick(item) }
        }
    }

    companion object {
        private const val TYPE_DIRECT = 0
        private const val TYPE_HISTORY = 1

        private val DIFF = object : DiffUtil.ItemCallback<HomeViewModel.SearchSuggestion>() {
            override fun areItemsTheSame(
                a: HomeViewModel.SearchSuggestion,
                b: HomeViewModel.SearchSuggestion
            ): Boolean = when {
                a is HomeViewModel.SearchSuggestion.DirectSearch &&
                    b is HomeViewModel.SearchSuggestion.DirectSearch ->
                    a.query == b.query && a.engine.id == b.engine.id
                a is HomeViewModel.SearchSuggestion.History &&
                    b is HomeViewModel.SearchSuggestion.History ->
                    a.url == b.url
                else -> false
            }

            override fun areContentsTheSame(
                a: HomeViewModel.SearchSuggestion,
                b: HomeViewModel.SearchSuggestion
            ): Boolean = a == b
        }
    }
}
