package com.browser.app.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.browser.app.R
import com.browser.app.data.entity.BookmarkEntity
import com.browser.app.databinding.FragmentBookmarksBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class BookmarksFragment : Fragment() {
    private var _binding: FragmentBookmarksBinding? = null
    private val binding get() = _binding!!
    private val viewModel: BookmarksViewModel by viewModels()
    private lateinit var adapter: BookmarkAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBookmarksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeBookmarks()
    }

    private fun setupRecyclerView() {
        adapter = BookmarkAdapter(
            onItemClick = { bookmark ->
                val action = BookmarksFragmentDirections
                    .actionBookmarksFragmentToWebviewFragment(bookmark.url)
                findNavController().navigate(action)
            },
            onLongClick = { bookmark -> showDeleteDialog(bookmark) }
        )
        binding.bookmarksList.layoutManager = LinearLayoutManager(requireContext())
        binding.bookmarksList.adapter = adapter
    }

    private fun observeBookmarks() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.bookmarks.collect { bookmarks ->
                    adapter.submitList(bookmarks)
                    binding.emptyText.visibility =
                        if (bookmarks.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
    }

    private fun showDeleteDialog(bookmark: BookmarkEntity) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.dialog_delete_bookmark_title)
            .setMessage(R.string.dialog_delete_bookmark_message)
            .setPositiveButton(R.string.dialog_delete) { _, _ -> viewModel.removeBookmark(bookmark.url) }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
