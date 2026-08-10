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
import com.browser.app.R
import com.browser.app.databinding.FragmentWindowsBinding
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
            onItemClick = { window -> navigateToWebview(window.url, window.id) },
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
                    binding.windowCount.text = getString(R.string.window_count, count)
                }
            }
        }
    }

    private fun setupAddButton() {
        binding.addWindowBtn.setOnClickListener { navigateToWebview("https://www.baidu.com", 0L) }
    }

    private fun navigateToWebview(url: String, windowId: Long) {
        val action = WindowsFragmentDirections.actionWindowsFragmentToWebviewFragment(url, windowId)
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
