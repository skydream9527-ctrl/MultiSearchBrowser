package com.browser.app.repository

import com.browser.app.data.dao.BookmarkDao
import com.browser.app.data.entity.BookmarkEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookmarkRepository @Inject constructor(private val bookmarkDao: BookmarkDao) {
    fun getAllBookmarks(): Flow<List<BookmarkEntity>> = bookmarkDao.getAllBookmarks()

    fun isBookmarked(url: String): Flow<Boolean> = bookmarkDao.isBookmarked(url)

    suspend fun addBookmark(title: String, url: String) {
        bookmarkDao.insert(BookmarkEntity(title = title, url = url))
    }

    suspend fun removeBookmark(url: String) {
        bookmarkDao.deleteByUrl(url)
    }

    suspend fun toggleBookmark(title: String, url: String): Boolean {
        val existing = bookmarkDao.getByUrl(url)
        return if (existing != null) {
            bookmarkDao.delete(existing)
            false
        } else {
            bookmarkDao.insert(BookmarkEntity(title = title, url = url))
            true
        }
    }
}
