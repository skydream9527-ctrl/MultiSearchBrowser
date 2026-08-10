package com.browser.app.ui.downloads

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.browser.app.R
import com.browser.app.data.entity.DownloadEntity
import com.browser.app.databinding.FragmentDownloadsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DownloadsFragment : Fragment() {
    private var _binding: FragmentDownloadsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DownloadsViewModel by viewModels()
    private lateinit var adapter: DownloadRecordAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDownloadsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeDownloads()
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
    }

    private fun setupRecyclerView() {
        adapter = DownloadRecordAdapter(
            onItemClick = { entity -> openFile(entity) },
            onDeleteClick = { entity -> showDeleteDialog(entity) }
        )
        binding.downloadsList.layoutManager = LinearLayoutManager(requireContext())
        binding.downloadsList.adapter = adapter
    }

    private fun observeDownloads() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.downloads.collect { list ->
                    adapter.submitList(list)
                    binding.emptyText.visibility =
                        if (list.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
    }

    private fun openFile(entity: DownloadEntity) {
        viewModel.openDownloadIntent(entity) { intent ->
            if (intent != null && intent.resolveActivity(requireContext().packageManager) != null) {
                startActivity(intent)
            } else {
                Toast.makeText(
                    requireContext(),
                    R.string.downloads_open_failed,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun showDeleteDialog(entity: DownloadEntity) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.downloads_delete)
            .setMessage(R.string.downloads_delete_message)
            .setPositiveButton(R.string.dialog_delete) { _, _ -> viewModel.deleteDownload(entity) }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
