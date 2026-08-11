package com.browser.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.browser.app.data.entity.BookmarkEntity
import com.browser.app.repository.BookmarkRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BookmarksViewModel @Inject constructor(
    private val bookmarkRepository: BookmarkRepository
) : ViewModel() {

    /** 当前选中的文件夹过滤：null 表示"全部"，"" 表示"未分组"，其他字符串是文件夹名 */
    private val _selectedFolder = MutableStateFlow<String?>(null)
    val selectedFolder: StateFlow<String?> = _selectedFolder.asStateFlow()

    private val _folders = MutableStateFlow<List<String>>(emptyList())

    /** 收藏列表：根据选中的 folder 过滤后返回 */
    val bookmarks: StateFlow<List<BookmarkEntity>> = combine(
        bookmarkRepository.getAllBookmarks(),
        _selectedFolder,
        _folders
    ) { all, selected, _ ->
        when (selected) {
            null -> all // 全部
            "" -> all.filter { it.folder.isEmpty() } // 未分组
            else -> all.filter { it.folder == selected }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** 文件夹列表（含"全部" / "未分组"两个虚拟项，UI 上单独渲染） */
    val folders: StateFlow<List<String>> = combine(
        bookmarkRepository.observeFolders(),
        bookmarkRepository.getAllBookmarks()
    ) { foldersFromDb, all ->
        // 文件夹为空时也允许 "未分组" 出现，但只有当收藏列表非空时才有意义
        if (all.isEmpty()) emptyList() else foldersFromDb
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectFolder(folder: String?) {
        _selectedFolder.value = folder
    }

    fun removeBookmark(url: String) {
        viewModelScope.launch {
            bookmarkRepository.removeBookmark(url)
        }
    }

    fun updateBookmark(bookmark: BookmarkEntity) {
        viewModelScope.launch {
            bookmarkRepository.updateBookmark(bookmark)
        }
    }

    fun moveBookmark(id: Long, folder: String) {
        viewModelScope.launch {
            bookmarkRepository.moveFolder(id, folder)
        }
    }
}
