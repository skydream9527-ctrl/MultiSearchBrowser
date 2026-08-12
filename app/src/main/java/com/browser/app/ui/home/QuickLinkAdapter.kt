package com.browser.app.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.browser.app.databinding.ItemQuickLinkBinding
import com.browser.app.utils.SearchEngine

/** 引擎列表项：包含引擎信息与选中状态，驱动 DiffUtil 增量刷新 */
data class EngineItem(
    val engine: SearchEngine,
    val isSelected: Boolean
)

/**
 * 合并版引擎选择适配器：同时承担原 ChipGroup 选引擎 + RecyclerView 快捷入口的职责。
 * 使用 ListAdapter + DiffUtil 实现增量更新，选中状态变化时仅刷新受影响项。
 */
class QuickLinkAdapter(
    private val onItemClick: (SearchEngine) -> Unit
) : ListAdapter<EngineItem, QuickLinkAdapter.ViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemQuickLinkBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemQuickLinkBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: EngineItem) {
            val engine = item.engine
            binding.name.text = engine.name
            // 用引擎首字 + 引擎色作为圆形图标（替代原未设置的 ImageView）
            binding.engineIcon.text = engine.name.first().toString()
            val color = ContextCompat.getColor(binding.root.context, engine.colorResId)
            binding.engineIcon.setBackgroundColor(color)
            // 选中状态驱动 card_bg_selector 切换边框
            binding.root.isSelected = item.isSelected
            binding.root.setOnClickListener { onItemClick(engine) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<EngineItem>() {
            override fun areItemsTheSame(old: EngineItem, new: EngineItem) =
                old.engine.id == new.engine.id

            override fun areContentsTheSame(old: EngineItem, new: EngineItem) =
                old.isSelected == new.isSelected
        }
    }
}
