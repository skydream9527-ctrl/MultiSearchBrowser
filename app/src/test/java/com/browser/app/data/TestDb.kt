package com.browser.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.browser.app.data.dao.BookmarkDao
import com.browser.app.data.dao.HistoryDao
import com.browser.app.data.dao.WindowDao

/**
 * 单元测试共用的内存数据库工厂。
 *
 * 使用 inMemoryDatabaseBuilder，进程退出 / 测试结束后自动清空，
 * 既不会污染设备存储，也不需要做 Migration 兼容，每个用例都能拿到干净的初始状态。
 */
object TestDb {

    /** 创建一次性内存 BrowserDatabase，并返回三个 DAO 的三元组。 */
    fun build(): BrowserDatabase =
        Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            BrowserDatabase::class.java
        )
            // 测试中允许主线程做 IO，避免每个用例都写 runBlocking + Dispatchers 切换
            .allowMainThreadQueries()
            .build()

    /** 一次性拿到三个 DAO，省去每个测试用例都重复写 db.xxxDao() */
    data class Daos(
        val bookmarkDao: BookmarkDao,
        val historyDao: HistoryDao,
        val windowDao: WindowDao
    )

    fun daos(db: BrowserDatabase) = Daos(
        bookmarkDao = db.bookmarkDao(),
        historyDao = db.historyDao(),
        windowDao = db.windowDao()
    )
}
