package com.browser.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "windows")
data class WindowEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val url: String,
    val timestamp: Long = System.currentTimeMillis(),
    /** 无痕 tab：不写历史、关闭 Cookie、退出时清理缓存 */
    val isIncognito: Boolean = false
)
