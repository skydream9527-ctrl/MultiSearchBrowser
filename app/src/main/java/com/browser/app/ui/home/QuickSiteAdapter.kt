package com.browser.app.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.browser.app.databinding.ItemQuickLinkBinding

/**
 * 首页"常用网站"网格适配器，复用 item_quick_link 布局。
 * 点击直接走外层 navigateToWebview(url)。
 */
class QuickSiteAdapter(
    private val onItemClick: (QuickSite) -> Unit
) : RecyclerView.Adapter<QuickSiteAdapter.ViewHolder>() {

    private val items = QuickSite.DEFAULTS.toMutableList()

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

        fun bind(site: QuickSite) {
            binding.name.text = binding.root.context.getString(site.nameResId)
            // 用首字母作为占位图标，避免引入大量 site logo 资源
            binding.icon.setBackgroundColor(
                binding.root.context.getColor(site.colorResId)
            )
            binding.root.setOnClickListener { onItemClick(site) }
        }
    }
}
