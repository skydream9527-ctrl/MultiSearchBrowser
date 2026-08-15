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

    @Update
    suspend fun update(history: HistoryEntity)

    @Delete
    suspend fun delete(history: HistoryEntity)

    @Query("DELETE FROM history")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM history")
    fun getCount(): Flow<Int>

    /**
     * UPSERT 语义：相同 url 只更新 title 与 timestamp，保留原 id。
     * 避免 delete+insert 导致的 id 跳号与历史顺序异常。
     */
    @androidx.room.Transaction
    suspend fun upsert(title: String, url: String) {
        val existing = getByUrl(url)
        if (existing != null) {
            update(existing.copy(title = title, timestamp = System.currentTimeMillis()))
        } else {
            insert(HistoryEntity(title = title, url = url))
        }
    }
}
