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
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.browser.app.R
import com.browser.app.data.entity.BookmarkEntity
import com.browser.app.databinding.FragmentBookmarksBinding
import com.browser.app.databinding.ItemBookmarkBinding
import com.browser.app.utils.navigateToWebview
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
                findNavController().navigateToWebview(bookmark.url)
            },
            onLongClick = { bookmark ->
                showDeleteDialog(bookmark)
            }
        )
        binding.bookmarksList.layoutManager = LinearLayoutManager(requireContext())
        binding.bookmarksList.adapter = adapter
    }

    private fun observeBookmarks() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.bookmarks.collect { bookmarks ->
                    adapter.submitList(bookmarks)
                    binding.emptyText.visibility = if (bookmarks.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
    }

    private fun showDeleteDialog(bookmark: BookmarkEntity) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete_bookmark)
            .setMessage(R.string.confirm_delete_bookmark)
            .setPositiveButton(R.string.action_delete) { _, _ ->
                viewModel.deleteBookmark(bookmark.url)
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class BookmarkAdapter(
    private val onItemClick: (BookmarkEntity) -> Unit,
    private val onLongClick: (BookmarkEntity) -> Unit
) : ListAdapter<BookmarkEntity, BookmarkAdapter.ViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemBookmarkBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemBookmarkBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(bookmark: BookmarkEntity) {
            binding.title.text = bookmark.title.ifEmpty { getString(R.string.no_title) }
            binding.url.text = bookmark.url
            binding.root.setOnClickListener { onItemClick(bookmark) }
            binding.root.setOnLongClickListener {
                onLongClick(bookmark)
                true
            }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<BookmarkEntity>() {
            override fun areItemsTheSame(oldItem: BookmarkEntity, newItem: BookmarkEntity) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: BookmarkEntity, newItem: BookmarkEntity) = oldItem == newItem
        }
    }
}
