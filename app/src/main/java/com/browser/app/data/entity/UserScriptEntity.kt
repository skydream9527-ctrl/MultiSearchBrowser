package com.browser.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_scripts")
data class UserScriptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val pattern: String = ".*",
    val description: String = "",
    val code: String,
    val enabled: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)
