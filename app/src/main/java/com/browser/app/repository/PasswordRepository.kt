package com.browser.app.repository

import com.browser.app.data.dao.PasswordDao
import com.browser.app.data.entity.PasswordEntity
import com.browser.app.utils.CryptoUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * v2.0.0 安全加固：Repository 层负责加解密，对上层屏蔽明文/密文差异。
 * - 入参为明文密码，Repository 调用 CryptoUtils 加密后入库
 * - 出参为已解密的明文密码（仅在内存中短暂存在）
 * - 数据库中只存 base64(IV || ciphertext) 形式的密文
 */
class PasswordRepository(private val dao: PasswordDao) {

    /** 返回明文密码列表（已解密） */
    fun getAllPasswords(): Flow<List<PasswordEntity>> =
        dao.getAllPasswords().map { list -> list.map { it.decrypted() } }

    fun searchPasswords(query: String): Flow<List<PasswordEntity>> =
        dao.searchPasswords(query).map { list -> list.map { it.decrypted() } }

    /** 入参为明文密码，加密后入库 */
    suspend fun addPassword(site: String, username: String, plainPassword: String, url: String = ""): Long {
        val cipher = CryptoUtils.encrypt(plainPassword)
        return dao.insert(PasswordEntity(
            site = site, username = username, encryptedPassword = cipher, url = url
        ))
    }

    suspend fun update(password: PasswordEntity) {
        // password 中的 encryptedPassword 是上层传来的明文，需重新加密
        val reEncrypted = password.copy(encryptedPassword = CryptoUtils.encrypt(password.encryptedPassword))
        dao.update(reEncrypted)
    }

    suspend fun delete(password: PasswordEntity) = dao.delete(password)
    suspend fun deleteById(id: Long) = dao.deleteById(id)

    /** 按 URL 查找并解密 */
    suspend fun getByUrl(url: String): PasswordEntity? = dao.getByUrl(url)?.decrypted()

    /** 把数据库中的密文 Entity 解密为明文 Entity */
    private fun PasswordEntity.decrypted(): PasswordEntity {
        val plain = CryptoUtils.decrypt(this.encryptedPassword) ?: return this
        return this.copy(encryptedPassword = plain)
    }
}
