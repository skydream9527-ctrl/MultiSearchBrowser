package com.browser.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val sourceUrl: String = "",
    val sourceTitle: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
