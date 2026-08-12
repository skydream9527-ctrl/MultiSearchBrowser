package com.browser.app.repository

import com.browser.app.data.dao.UserScriptDao
import com.browser.app.data.entity.UserScriptEntity
import kotlinx.coroutines.flow.Flow

class UserScriptRepository(private val dao: UserScriptDao) {
    fun getAllScripts(): Flow<List<UserScriptEntity>> = dao.getAllScripts()
    suspend fun getEnabledScripts(): List<UserScriptEntity> = dao.getEnabledScripts()

    suspend fun addScript(name: String, pattern: String, code: String, description: String = ""): Long {
        return dao.insert(UserScriptEntity(name = name, pattern = pattern, code = code, description = description))
    }

    suspend fun update(script: UserScriptEntity) = dao.update(script)
    suspend fun delete(script: UserScriptEntity) = dao.delete(script)
    suspend fun deleteById(id: Long) = dao.deleteById(id)
    suspend fun setEnabled(id: Long, enabled: Boolean) = dao.setEnabled(id, enabled)
}
