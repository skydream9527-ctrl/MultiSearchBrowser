package com.browser.app.ui.tabs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.browser.app.R
import com.browser.app.data.BrowserDatabase
import com.browser.app.data.entity.WindowEntity
import com.browser.app.databinding.FragmentWindowsBinding
import com.browser.app.databinding.ItemWindowBinding
import com.browser.app.repository.WindowRepository
import com.browser.app.utils.navigateToWebview
import kotlinx.coroutines.launch

class WindowsFragment : Fragment() {
    private var _binding: FragmentWindowsBinding? = null
    private val binding get() = _binding!!
    private lateinit var windowRepository: WindowRepository
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
        val db = BrowserDatabase.getInstance(requireContext())
        windowRepository = WindowRepository(db.windowDao())

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
                lifecycleScope.launch {
                    windowRepository.deleteWindow(window)
                }
            }
        )
        binding.windowsList.layoutManager = LinearLayoutManager(requireContext())
        binding.windowsList.adapter = adapter
    }

    private fun observeWindows() {
        lifecycleScope.launch {
            windowRepository.getAllWindows().collect { windows ->
                adapter.submitList(windows)
                binding.emptyText.visibility = if (windows.isEmpty()) View.VISIBLE else View.GONE
                // 窗口数由列表派生，避免之前同时订阅两个 Flow 的浪费
                binding.windowCount.text = "${windows.size} 个窗口"
            }
        }
    }

    private fun setupAddButton() {
        binding.addWindowBtn.setOnClickListener {
            // 新建窗口：先入库拿到 id，再带 id 跳 webview
            viewLifecycleOwner.lifecycleScope.launch {
                val id = windowRepository.addWindow(title = "新窗口", url = "https://www.baidu.com")
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
) : RecyclerView.Adapter<WindowAdapter.ViewHolder>() {

    private var items: List<WindowEntity> = emptyList()

    fun submitList(newItems: List<WindowEntity>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemWindowBinding.inflate(
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

    inner class ViewHolder(private val binding: ItemWindowBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(window: WindowEntity) {
            binding.title.text = window.title.ifEmpty { "无标题" }
            binding.url.text = window.url
            binding.root.setOnClickListener { onItemClick(window) }
            binding.closeBtn.setOnClickListener { onCloseClick(window) }
        }
    }
}