package com.browser.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    exportSchema = true  // v2.0.0: 导出 schema 用于 Migration 验证
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

        /**
         * v2.0.0: v1 → v2 显式 Migration。
         * 新增 5 张表（rss_feeds / rss_items / notes / passwords / user_scripts），
         * 保留 v1 已有的 history / bookmarks / windows 表数据不丢失。
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // RSS 订阅源表
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `rss_feeds` (
                        `id` INTEGER NOT NULL,
                        `name` TEXT NOT NULL,
                        `url` TEXT NOT NULL,
                        `lastFetched` INTEGER NOT NULL,
                        `enabled` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())
                // RSS 条目表
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `rss_items` (
                        `id` INTEGER NOT NULL,
                        `feedId` INTEGER NOT NULL,
                        `guid` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `link` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `pubDate` INTEGER NOT NULL,
                        `isRead` INTEGER NOT NULL,
                        `source` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_rss_items_feedId` ON `rss_items` (`feedId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_rss_items_guid` ON `rss_items` (`guid`)")
                // 笔记表
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `notes` (
                        `id` INTEGER NOT NULL,
                        `text` TEXT NOT NULL,
                        `sourceUrl` TEXT NOT NULL,
                        `sourceTitle` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())
                // 密码表
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `passwords` (
                        `id` INTEGER NOT NULL,
                        `site` TEXT NOT NULL,
                        `username` TEXT NOT NULL,
                        `encryptedPassword` TEXT NOT NULL,
                        `url` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())
                // 用户脚本表
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `user_scripts` (
                        `id` INTEGER NOT NULL,
                        `name` TEXT NOT NULL,
                        `pattern` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `code` TEXT NOT NULL,
                        `enabled` INTEGER NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())
            }
        }

        fun getInstance(context: Context): BrowserDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BrowserDatabase::class.java,
                    "browser_database"
                )
                    // v2.0.0: 显式 Migration 替代 fallbackToDestructiveMigration
                    // 保留用户历史/书签/窗口数据，新增表通过 Migration 创建
                    .addMigrations(MIGRATION_1_2)
                    // 仅作为未知版本的最后兜底，避免 schema 突变时崩溃
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
