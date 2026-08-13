package com.browser.app.utils

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * v2.0.0 安全加固：基于 Android Keystore 的 AES-GCM 密码加密工具。
 *
 * 设计要点：
 * - 密钥由 Android Keystore 生成并保管，不出设备、不可导出
 * - 加密算法 AES/GCM/NoPadding，12 字节 IV，128 位 auth tag
 * - 密文格式：base64(IV || ciphertext)，与 PasswordEntity.encryptedPassword 兼容
 * - 设备未设置锁屏时 Keystore 密钥仍可用，但建议结合 BiometricPrompt 做二次验证
 *
 * 注意：Keystore 在卸载应用后清空，已存的密文将无法解密（这是预期行为）。
 */
object CryptoUtils {

    private const val KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "msb_master_key"
    private const val GCM_IV_LENGTH = 12 // bytes
    private const val GCM_TAG_LENGTH = 128 // bits

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        keyStore.getKey(KEY_ALIAS, null)?.let { return it as SecretKey }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        generator.init(spec)
        return generator.generateKey()
    }

    /** 加密明文，返回 base64(IV || ciphertext) */
    fun encrypt(plaintext: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val iv = cipher.iv
        val cipherBytes = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        val combined = iv + cipherBytes
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    /** 解密 base64(IV || ciphertext)，失败返回 null */
    fun decrypt(ciphertext: String): String? {
        return try {
            val combined = Base64.decode(ciphertext, Base64.NO_WRAP)
            if (combined.size <= GCM_IV_LENGTH) return null
            val iv = combined.copyOfRange(0, GCM_IV_LENGTH)
            val encrypted = combined.copyOfRange(GCM_IV_LENGTH, combined.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(GCM_TAG_LENGTH, iv))
            String(cipher.doFinal(encrypted), Charsets.UTF_8)
        } catch (_: Exception) {
            null
        }
    }
}
