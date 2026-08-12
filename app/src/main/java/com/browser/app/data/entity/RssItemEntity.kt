package com.browser.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rss_items")
data class RssItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val feedId: Long,
    val guid: String,
    val title: String,
    val link: String,
    val description: String = "",
    val pubDate: Long = 0,
    val isRead: Boolean = false,
    val source: String = ""
)
