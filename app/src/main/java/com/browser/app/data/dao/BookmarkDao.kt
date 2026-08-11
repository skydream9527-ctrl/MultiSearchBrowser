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

    @Update
    suspend fun update(bookmark: BookmarkEntity)

    @Query("UPDATE bookmarks SET folder = :folder WHERE id = :id")
    suspend fun moveFolder(id: Long, folder: String)

    @Query("SELECT DISTINCT folder FROM bookmarks WHERE folder != '' ORDER BY folder")
    fun observeFolders(): Flow<List<String>>

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE url = :url)")
    fun isBookmarked(url: String): Flow<Boolean>
}