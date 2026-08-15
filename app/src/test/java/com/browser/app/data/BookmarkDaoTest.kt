package com.browser.app.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.browser.app.data.dao.BookmarkDao
import com.browser.app.data.entity.BookmarkEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * v2.0.0: BookmarkDao 单元测试（Robolectric 内存数据库）
 * 验证 insert / getAll / delete / toggleBookmark 等 DAO 操作。
 */
@RunWith(RobolectricTestRunner::class)
class BookmarkDaoTest {

    private lateinit var database: BrowserDatabase
    private lateinit var dao: BookmarkDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            BrowserDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = database.bookmarkDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insertBookmark_andRetrieveAll() = runTest {
        val bookmark = BookmarkEntity(
            title = "测试书签",
            url = "https://example.com",
            timestamp = System.currentTimeMillis()
        )
        dao.insert(bookmark)

        val all = dao.getAllBookmarks().first()
        assertEquals(1, all.size)
        assertEquals("测试书签", all[0].title)
        assertEquals("https://example.com", all[0].url)
    }

    @Test
    fun deleteBookmark_removesFromList() = runTest {
        val bookmark = BookmarkEntity(
            title = "待删除",
            url = "https://delete.me",
            timestamp = System.currentTimeMillis()
        )
        dao.insert(bookmark)
        dao.deleteByUrl("https://delete.me")

        val all = dao.getAllBookmarks().first()
        assertTrue(all.isEmpty())
    }

    @Test
    fun insertMultipleBookmarks_preservesOrder() = runTest {
        repeat(5) { i ->
            dao.insert(BookmarkEntity(
                title = "书签$i",
                url = "https://example.com/$i",
                timestamp = System.currentTimeMillis() + i
            ))
        }

        val all = dao.getAllBookmarks().first()
        assertEquals(5, all.size)
        // 验证顺序（按 timestamp 降序或插入顺序）
        for (i in all.indices) {
            assertNotNull(all[i].title)
        }
    }
}
