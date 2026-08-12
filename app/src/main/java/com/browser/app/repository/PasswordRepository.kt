package com.browser.app.repository

import com.browser.app.data.dao.PasswordDao
import com.browser.app.data.entity.PasswordEntity
import kotlinx.coroutines.flow.Flow

class PasswordRepository(private val dao: PasswordDao) {
    fun getAllPasswords(): Flow<List<PasswordEntity>> = dao.getAllPasswords()
    fun searchPasswords(query: String): Flow<List<PasswordEntity>> = dao.searchPasswords(query)

    suspend fun addPassword(site: String, username: String, encryptedPassword: String, url: String = ""): Long {
        return dao.insert(PasswordEntity(site = site, username = username, encryptedPassword = encryptedPassword, url = url))
    }

    suspend fun update(password: PasswordEntity) = dao.update(password)
    suspend fun delete(password: PasswordEntity) = dao.delete(password)
    suspend fun deleteById(id: Long) = dao.deleteById(id)
    suspend fun getByUrl(url: String): PasswordEntity? = dao.getByUrl(url)
}
