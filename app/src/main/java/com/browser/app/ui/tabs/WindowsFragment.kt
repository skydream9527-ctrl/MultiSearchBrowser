package com.browser.app.ui.tabs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.browser.app.data.entity.WindowEntity
import com.browser.app.databinding.FragmentWindowsBinding
import com.browser.app.databinding.ItemWindowBinding
import com.browser.app.utils.navigateToWebview
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class WindowsFragment : Fragment() {
    private var _binding: FragmentWindowsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: WindowsViewModel by viewModels()
    private lateinit var adapter: WindowAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWindowsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeWindows()
        setupAddButton()
    }

    private fun setupRecyclerView() {
        adapter = WindowAdapter(
            onItemClick = { window ->
                // 点击窗口项：带上 windowId 进入 webview，浏览过程会回写 url/title
                findNavController().navigateToWebview(window.url, window.id)
            },
            onCloseClick = { window ->
                viewModel.deleteWindow(window)
            }
        )
        binding.windowsList.layoutManager = LinearLayoutManager(requireContext())
        binding.windowsList.adapter = adapter
    }

    private fun observeWindows() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.windows.collect { windows ->
                    adapter.submitList(windows)
                    binding.emptyText.visibility = if (windows.isEmpty()) View.VISIBLE else View.GONE
                    // 窗口数由列表派生，避免同时订阅两个 Flow 的浪费
                    binding.windowCount.text = getString(R.string.window_count_format, windows.size)
                }
            }
        }
    }

    private fun setupAddButton() {
        binding.addWindowBtn.setOnClickListener {
            // 新建窗口：先入库拿到 id，再带 id 跳 webview
            viewLifecycleOwner.lifecycleScope.launch {
                val id = viewModel.addWindow(
                    title = getString(R.string.new_window_default_title),
                    url = "https://www.baidu.com"
                )
                findNavController().navigateToWebview("https://www.baidu.com", id)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

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
            binding.title.text = window.title.ifEmpty { getString(R.string.no_title) }
            binding.url.text = window.url
            binding.root.setOnClickListener { onItemClick(window) }
            binding.closeBtn.setOnClickListener { onCloseClick(window) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<WindowEntity>() {
            override fun areItemsTheSame(oldItem: WindowEntity, newItem: WindowEntity) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: WindowEntity, newItem: WindowEntity) = oldItem == newItem
        }
    }
}
