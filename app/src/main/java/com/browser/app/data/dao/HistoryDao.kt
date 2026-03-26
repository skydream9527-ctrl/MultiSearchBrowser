package com.browser.app.data.dao

import androidx.room.*
import com.browser.app.data.entity.HistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history WHERE url = :url LIMIT 1")
    suspend fun getByUrl(url: String): HistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: HistoryEntity)

    @Delete
    suspend fun delete(history: HistoryEntity)

    @Query("DELETE FROM history")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM history")
    fun getCount(): Flow<Int>
}