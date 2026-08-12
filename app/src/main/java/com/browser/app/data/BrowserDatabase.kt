package com.browser.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.browser.app.data.dao.BookmarkDao
import com.browser.app.data.dao.HistoryDao
import com.browser.app.data.dao.NoteDao
import com.browser.app.data.dao.PasswordDao
import com.browser.app.data.dao.RssDao
import com.browser.app.data.dao.UserScriptDao
import com.browser.app.data.dao.WindowDao
import com.browser.app.data.entity.BookmarkEntity
import com.browser.app.data.entity.HistoryEntity
import com.browser.app.data.entity.NoteEntity
import com.browser.app.data.entity.PasswordEntity
import com.browser.app.data.entity.RssEntity
import com.browser.app.data.entity.RssItemEntity
import com.browser.app.data.entity.UserScriptEntity
import com.browser.app.data.entity.WindowEntity

@Database(
    entities = [
        HistoryEntity::class,
        BookmarkEntity::class,
        WindowEntity::class,
        // v1.9.0: 新增实体
        RssEntity::class,
        RssItemEntity::class,
        NoteEntity::class,
        PasswordEntity::class,
        UserScriptEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class BrowserDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun windowDao(): WindowDao
    // v1.9.0: 新增 DAO
    abstract fun rssDao(): RssDao
    abstract fun noteDao(): NoteDao
    abstract fun passwordDao(): PasswordDao
    abstract fun userScriptDao(): UserScriptDao

    companion object {
        @Volatile
        private var INSTANCE: BrowserDatabase? = null

        fun getInstance(context: Context): BrowserDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BrowserDatabase::class.java,
                    "browser_database"
                )
                    // schema 变更时显式丢弃旧数据，避免应用崩溃。
                    // 正式发布前应改为 Migration 策略以保留用户数据。
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}