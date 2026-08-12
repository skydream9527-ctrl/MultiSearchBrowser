package com.browser.app.data.dao

import androidx.room.*
import com.browser.app.data.entity.BookmarkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY timestamp DESC")
    fun getAllBookmarks(): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE url = :url LIMIT 1")
    suspend fun getByUrl(url: String): BookmarkEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bookmark: BookmarkEntity)

    @Delete
    suspend fun delete(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE url = :url")
    suspend fun deleteByUrl(url: String)

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE url = :url)")
    fun isBookmarked(url: String): Flow<Boolean>

    /**
     * 原子化切换收藏状态：用 @Transaction 包装 查询+写入，
     * 避免快速连点导致的重复添加竞态。
     */
    @androidx.room.Transaction
    suspend fun toggleByUrl(title: String, url: String): Boolean {
        val existing = getByUrl(url)
        return if (existing != null) {
            delete(existing)
            false
        } else {
            insert(BookmarkEntity(title = title, url = url))
            true
        }
    }
}