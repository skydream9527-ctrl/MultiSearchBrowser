package com.browser.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "passwords")
data class PasswordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val site: String,
    val username: String,
    val encryptedPassword: String,  // AES-256 加密后的密码
    val url: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
