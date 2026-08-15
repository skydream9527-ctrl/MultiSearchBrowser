package com.browser.app.data.dao

import androidx.room.*
import com.browser.app.data.entity.WindowEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WindowDao {
    @Query("SELECT * FROM windows ORDER BY timestamp DESC")
    fun getAllWindows(): Flow<List<WindowEntity>>

    @Query("SELECT * FROM windows WHERE id = :id")
    suspend fun getById(id: Long): WindowEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(window: WindowEntity): Long

    @Update
    suspend fun update(window: WindowEntity)

    @Delete
    suspend fun delete(window: WindowEntity)

    @Query("DELETE FROM windows WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM windows")
    fun getCount(): Flow<Int>
}
