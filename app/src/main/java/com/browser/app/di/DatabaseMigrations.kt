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

    val ALL: Array<Migration> = arrayOf(MIGRATION_1_2)
}
