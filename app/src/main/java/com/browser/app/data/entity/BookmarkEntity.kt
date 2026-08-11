package com.browser.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val url: String,
    val timestamp: Long = System.currentTimeMillis(),
    /** 站点图标 URL，v2 新增；旧数据为 null */
    val faviconUrl: String? = null,
    /** 所属文件夹名，v5 新增；空字符串表示"未分组" */
    val folder: String = ""
) {
    companion object {
        /** 未分组标记：UI 上显示为"全部" */
        const val NO_FOLDER = ""
    }
}

