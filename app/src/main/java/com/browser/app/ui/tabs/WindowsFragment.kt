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
import androidx.recyclerview.widget.LinearLayoutManager
import com.browser.app.data.entity.WindowEntity
import com.browser.app.databinding.FragmentWindowsBinding
import com.browser.app.databinding.ItemWindowBinding
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
            onItemClick = { window -> navigateToWebview(window.url) },
            onCloseClick = { window -> viewModel.deleteWindow(window) }
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
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.count.collect { count ->
                    binding.windowCount.text = "$count 个窗口"
                }
            }
        }
    }

    private fun setupAddButton() {
        binding.addWindowBtn.setOnClickListener { navigateToWebview("https://www.baidu.com") }
    }

    private fun navigateToWebview(url: String) {
        val action = WindowsFragmentDirections.actionWindowsFragmentToWebviewFragment(url)
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class WindowAdapter(
    private val onItemClick: (WindowEntity) -> Unit,
    private val onCloseClick: (WindowEntity) -> Unit
) : androidx.recyclerview.widget.ListAdapter<WindowEntity, WindowAdapter.ViewHolder>(DIFF) {

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
        androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root) {

        fun bind(window: WindowEntity) {
            binding.title.text = window.title.ifEmpty { "无标题" }
            binding.url.text = window.url
            binding.root.setOnClickListener { onItemClick(window) }
            binding.closeBtn.setOnClickListener { onCloseClick(window) }
        }
    }

    companion object {
        private val DIFF = object : androidx.recyclerview.widget.DiffUtil.ItemCallback<WindowEntity>() {
            override fun areItemsTheSame(oldItem: WindowEntity, newItem: WindowEntity): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: WindowEntity, newItem: WindowEntity): Boolean =
                oldItem == newItem
        }
    }
}
