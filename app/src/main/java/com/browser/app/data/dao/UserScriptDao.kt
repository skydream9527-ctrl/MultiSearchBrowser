package com.browser.app.data.dao

import androidx.room.*
import com.browser.app.data.entity.UserScriptEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserScriptDao {
    @Query("SELECT * FROM user_scripts ORDER BY name")
    fun getAllScripts(): Flow<List<UserScriptEntity>>

    @Query("SELECT * FROM user_scripts WHERE enabled = 1")
    suspend fun getEnabledScripts(): List<UserScriptEntity>

    @Insert
    suspend fun insert(script: UserScriptEntity): Long

    @Update
    suspend fun update(script: UserScriptEntity)

    @Delete
    suspend fun delete(script: UserScriptEntity)

    @Query("DELETE FROM user_scripts WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE user_scripts SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)
}
