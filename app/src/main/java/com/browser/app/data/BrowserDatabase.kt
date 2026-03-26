package com.browser.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.browser.app.data.dao.BookmarkDao
import com.browser.app.data.dao.HistoryDao
import com.browser.app.data.dao.WindowDao
import com.browser.app.data.entity.BookmarkEntity
import com.browser.app.data.entity.HistoryEntity
import com.browser.app.data.entity.WindowEntity

@Database(
    entities = [
        HistoryEntity::class,
        BookmarkEntity::class,
        WindowEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class BrowserDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun windowDao(): WindowDao

    companion object {
        @Volatile
        private var INSTANCE: BrowserDatabase? = null

        fun getInstance(context: Context): BrowserDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BrowserDatabase::class.java,
                    "browser_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}