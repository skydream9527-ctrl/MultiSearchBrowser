package com.browser.app.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.browser.app.data.dao.HistoryDao
import com.browser.app.data.entity.HistoryEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * HistoryDao 单元测试（Robolectric 内存数据库）。
 * 验证 insert / getAllHistory / getByUrl / delete / deleteAll / upsert / getCount 等 DAO 操作。
 */
@RunWith(RobolectricTestRunner::class)
class HistoryDaoTest {

    private lateinit var database: BrowserDatabase
    private lateinit var dao: HistoryDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            BrowserDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = database.historyDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insert_andRetrieveAll() = runTest {
        val item = HistoryEntity(
            title = "测试页面",
            url = "https://example.com",
            timestamp = System.currentTimeMillis()
        )
        dao.insert(item)

        val all = dao.getAllHistory().first()
        assertEquals(1, all.size)
        assertEquals("测试页面", all[0].title)
        assertEquals("https://example.com", all[0].url)
    }

    @Test
    fun insertMultiple_orderedByTimestampDesc() = runTest {
        val base = System.currentTimeMillis()
        // 依次插入三条历史，timestamp 递增
        dao.insert(HistoryEntity(title = "旧", url = "https://old.com", timestamp = base))
        dao.insert(HistoryEntity(title = "中", url = "https://mid.com", timestamp = base + 1000))
        dao.insert(HistoryEntity(title = "新", url = "https://new.com", timestamp = base + 2000))

        val all = dao.getAllHistory().first()
        assertEquals(3, all.size)
        // getAllHistory SQL 中 ORDER BY timestamp DESC
        assertEquals("新", all[0].title)
        assertEquals("中", all[1].title)
        assertEquals("旧", all[2].title)
    }

    @Test
    fun getByUrl_returnsMatchingOrNull() = runTest {
        dao.insert(HistoryEntity(title = "Example", url = "https://example.com"))

        val found = dao.getByUrl("https://example.com")
        assertNotNull(found)
        assertEquals("Example", found?.title)

        val missing = dao.getByUrl("https://nonexistent.com")
        assertNull(missing)
    }

    @Test
    fun delete_removesEntry() = runTest {
        dao.insert(HistoryEntity(title = "待删除", url = "https://delete.me"))
        val inserted = dao.getByUrl("https://delete.me")!!

        dao.delete(inserted)

        assertTrue(dao.getAllHistory().first().isEmpty())
    }

    @Test
    fun deleteAll_clearsTable() = runTest {
        repeat(3) { i ->
            dao.insert(HistoryEntity(title = "历史$i", url = "https://example.com/$i"))
        }
        assertEquals(3, dao.getAllHistory().first().size)

        dao.deleteAll()

        assertTrue(dao.getAllHistory().first().isEmpty())
    }

    @Test
    fun upsert_existingUrl_updatesInPlace() = runTest {
        // 首次插入
        dao.upsert("原标题", "https://example.com")
        val first = dao.getByUrl("https://example.com")!!
        assertEquals("原标题", first.title)

        // 相同 URL 再次 upsert，应更新 title 且不新增记录
        dao.upsert("新标题", "https://example.com")

        val all = dao.getAllHistory().first()
        assertEquals("upsert 不应新增记录", 1, all.size)
        assertEquals("新标题", all[0].title)
        // UPSERT 语义：保留原 id
        assertEquals(first.id, all[0].id)
    }

    @Test
    fun upsert_newUrl_insertsEntry() = runTest {
        dao.upsert("标题A", "https://a.com")
        dao.upsert("标题B", "https://b.com")

        val all = dao.getAllHistory().first()
        assertEquals(2, all.size)
    }

    @Test
    fun getCount_reflectsTableSize() = runTest {
        assertEquals(0, dao.getCount().first())

        dao.insert(HistoryEntity(title = "A", url = "https://a.com"))
        dao.insert(HistoryEntity(title = "B", url = "https://b.com"))

        assertEquals(2, dao.getCount().first())
    }
}
