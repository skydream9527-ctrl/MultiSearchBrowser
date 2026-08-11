package com.browser.app.di

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room 数据库迁移集合。
 *
 * 新增字段或表时：
 * 1. 修改 Entity / 新增 Entity
 * 2. 在 BrowserDatabase 升 version
 * 3. 在此处新增 Migration（n -> n+1），写 ALTER TABLE / CREATE TABLE 语句
 * 4. DatabaseModule 已通过 addMigrations() 注册
 *
 * Schema JSON 会通过 KSP 自动导出到 app/schemas/，便于版本对比。
 */
object DatabaseMigrations {

    /** v1 -> v2：bookmarks 表新增 faviconUrl 列 */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE bookmarks ADD COLUMN faviconUrl TEXT")
        }
    }

    /** v2 -> v3：新增 downloads 表（应用内下载管理） */
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // 字段类型与 DownloadEntity 一一对应，避免 Room schema 校验失败
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS downloads (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    downloadId INTEGER NOT NULL,
                    title TEXT NOT NULL,
                    url TEXT NOT NULL,
                    mimetype TEXT,
                    localUri TEXT,
                    status INTEGER NOT NULL,
                    totalBytes INTEGER NOT NULL,
                    downloadedBytes INTEGER NOT NULL,
                    timestamp INTEGER NOT NULL
                )
                """.trimIndent()
            )
        }
    }

    /** v3 -> v4：windows 表新增 isIncognito 列（无痕 tab 标识） */
    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                "ALTER TABLE windows ADD COLUMN isIncognito INTEGER NOT NULL DEFAULT 0"
            )
        }
    }

    /** v4 -> v5：bookmarks 表新增 folder 列（收藏分组） */
    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                "ALTER TABLE bookmarks ADD COLUMN folder TEXT NOT NULL DEFAULT ''"
            )
        }
    }

    val ALL: Array<Migration> = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
}
