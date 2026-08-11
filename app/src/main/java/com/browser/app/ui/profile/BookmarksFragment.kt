package com.browser.app.ui.profile

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
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
import com.google.android.material.chip.Chip
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
        observeFolders()
    }

    private fun setupRecyclerView() {
        adapter = BookmarkAdapter(
            onItemClick = { bookmark ->
                val action = BookmarksFragmentDirections
                    .actionBookmarksFragmentToWebviewFragment(bookmark.url)
                findNavController().navigate(action)
            },
            onLongClick = { bookmark -> showEditOrDeleteDialog(bookmark) }
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

    private fun observeFolders() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.folders.collect { folders ->
                    renderFolderChips(folders)
                }
            }
        }
    }

    /** 文件夹 chip 行：永远包含「全部」+「未分组」+ DB 里的所有文件夹 */
    private fun renderFolderChips(folders: List<String>) {
        binding.folderChips.removeAllViews()
        val all = listOf<FolderItem>(
            FolderItem(null, getString(R.string.bookmark_folder_all)),
            FolderItem("", getString(R.string.bookmark_folder_unfiled))
        ) + folders.map { FolderItem(it, it) }
        all.forEach { item ->
            val chip = Chip(requireContext()).apply {
                text = item.label
                isCheckable = true
                tag = item.folder
                if (viewModel.selectedFolder.value == item.folder) {
                    isChecked = true
                    setChipStrokeColorResource(R.color.primary)
                    chipStrokeWidth = 2f
                }
                setOnClickListener {
                    viewModel.selectFolder(item.folder)
                }
            }
            binding.folderChips.addView(chip)
        }
    }

    private data class FolderItem(val folder: String?, val label: String)

    /**
     * 长按收藏弹出选择菜单：编辑 / 删除
     */
    private fun showEditOrDeleteDialog(bookmark: BookmarkEntity) {
        val options = arrayOf(
            getString(R.string.bookmark_edit_title),
            getString(R.string.dialog_delete_bookmark_title)
        )
        AlertDialog.Builder(requireContext())
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showEditDialog(bookmark)
                    1 -> showDeleteDialog(bookmark)
                }
            }
            .show()
    }

    /**
     * 编辑对话框：可改 title 与 folder
     */
    private fun showEditDialog(bookmark: BookmarkEntity) {
        val context = requireContext()
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 24)
        }
        val titleEdit = EditText(context).apply {
            hint = getString(R.string.bookmark_edit_name)
            inputType = InputType.TYPE_CLASS_TEXT
            setText(bookmark.title)
        }
        val folderEdit = EditText(context).apply {
            hint = getString(R.string.bookmark_edit_folder_hint)
            inputType = InputType.TYPE_CLASS_TEXT
            setText(bookmark.folder)
        }
        container.addView(titleEdit)
        container.addView(folderEdit)

        AlertDialog.Builder(context)
            .setTitle(R.string.bookmark_edit_title)
            .setView(container)
            .setPositiveButton(R.string.dialog_save) { _, _ ->
                val newTitle = titleEdit.text.toString().trim()
                val newFolder = folderEdit.text.toString().trim()
                viewModel.updateBookmark(
                    bookmark.copy(
                        title = newTitle.ifBlank { bookmark.url },
                        folder = newFolder
                    )
                )
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
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
