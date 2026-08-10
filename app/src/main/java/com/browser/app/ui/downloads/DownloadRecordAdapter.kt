package com.browser.app.ui.downloads

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.browser.app.R
import com.browser.app.data.entity.DownloadEntity
import com.browser.app.databinding.ItemDownloadBinding

class DownloadRecordAdapter(
    private val onItemClick: (DownloadEntity) -> Unit,
    private val onDeleteClick: (DownloadEntity) -> Unit
) : ListAdapter<DownloadEntity, DownloadRecordAdapter.ViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDownloadBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemDownloadBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(entity: DownloadEntity) {
            binding.title.text = entity.title.ifEmpty { entity.url }
            binding.url.text = entity.url
            bindStatus(entity)

            // 仅"已完成"状态显示 open 按钮，其它状态禁用
            val isFinished = entity.status == android.app.DownloadManager.STATUS_SUCCESSFUL
            binding.openBtn.isEnabled = isFinished
            binding.openBtn.alpha = if (isFinished) 1f else 0.4f
            binding.openBtn.setOnClickListener {
                if (isFinished) onItemClick(entity)
            }
            binding.deleteBtn.setOnClickListener { onDeleteClick(entity) }
        }

        private fun bindStatus(entity: DownloadEntity) {
            val ctx = itemView.context
            when (entity.status) {
                android.app.DownloadManager.STATUS_PENDING -> {
                    binding.progress.visibility = View.GONE
                    binding.status.text = ctx.getString(R.string.downloads_status_pending)
                }
                android.app.DownloadManager.STATUS_RUNNING -> {
                    binding.progress.visibility = View.VISIBLE
                    val percent = if (entity.totalBytes > 0) {
                        ((entity.downloadedBytes * 100f) / entity.totalBytes).toInt()
                    } else 0
                    binding.progress.progress = percent
                    binding.status.text =
                        ctx.getString(R.string.downloads_status_running, percent)
                }
                android.app.DownloadManager.STATUS_PAUSED -> {
                    binding.progress.visibility = View.GONE
                    binding.status.text = ctx.getString(R.string.downloads_status_paused)
                }
                android.app.DownloadManager.STATUS_SUCCESSFUL -> {
                    binding.progress.visibility = View.GONE
                    binding.status.text = ctx.getString(R.string.downloads_status_successful)
                }
                android.app.DownloadManager.STATUS_FAILED -> {
                    binding.progress.visibility = View.GONE
                    binding.status.text = ctx.getString(R.string.downloads_status_failed)
                }
                else -> {
                    binding.progress.visibility = View.GONE
                    binding.status.text = ""
                }
            }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<DownloadEntity>() {
            override fun areItemsTheSame(oldItem: DownloadEntity, newItem: DownloadEntity): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: DownloadEntity, newItem: DownloadEntity): Boolean =
                oldItem == newItem
        }
    }
}
