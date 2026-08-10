package com.browser.app.data.dao

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.browser.app.data.TestDb
import com.browser.app.data.entity.HistoryEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class HistoryDaoTest {

    private val db = TestDb.build()
    private val dao: HistoryDao = db.historyDao()

    @After
    fun teardown() = db.close()

    @Test
    fun insert_then_getAllHistory_returns_item() = runTest {
        dao.insert(HistoryEntity(title = "Kotlin", url = "https://kotlinlang.org"))

        val all = dao.getAllHistory().first()
        assertEquals(1, all.size)
        assertEquals("Kotlin", all.first().title)
    }

    @Test
    fun getByUrl_returns_entity_when_exists() = runTest {
        dao.insert(HistoryEntity(title = "Android", url = "https://developer.android.com"))

        val result = dao.getByUrl("https://developer.android.com")
        assertNotNull(result)
        assertEquals("Android", result?.title)
    }

    @Test
    fun getByUrl_returns_null_when_not_exists() = runTest {
        assertNull(dao.getByUrl("https://nope.example"))
    }

    @Test
    fun delete_removes_only_target_entity() = runTest {
        val a = HistoryEntity(title = "A", url = "https://a.example")
        val b = HistoryEntity(title = "B", url = "https://b.example")
        dao.insert(a)
        dao.insert(b)

        // getByUrl 拿到持久化后的实体（带 id），再调用 delete 才能按主键删除
        val persisted = dao.getByUrl("https://a.example")!!
        dao.delete(persisted)

        val all = dao.getAllHistory().first()
        assertEquals(1, all.size)
        assertEquals("https://b.example", all.first().url)
    }

    @Test
    fun deleteAll_clears_entire_table() = runTest {
        dao.insert(HistoryEntity(title = "1", url = "https://1.example"))
        dao.insert(HistoryEntity(title = "2", url = "https://2.example"))
        dao.insert(HistoryEntity(title = "3", url = "https://3.example"))

        dao.deleteAll()

        assertEquals(0, dao.getAllHistory().first().size)
        assertEquals(0, dao.getCount().first())
    }

    @Test
    fun getCount_emits_zero_initially_then_reflects_inserts() = runTest {
        assertEquals(0, dao.getCount().first())

        dao.insert(HistoryEntity(title = "x", url = "https://x.example"))
        dao.insert(HistoryEntity(title = "y", url = "https://y.example"))

        assertEquals(2, dao.getCount().first())
    }

    @Test
    fun getAllHistory_orders_by_timestamp_desc() = runTest {
        dao.insert(HistoryEntity(title = "older", url = "https://old.example", timestamp = 100L))
        dao.insert(HistoryEntity(title = "newer", url = "https://new.example", timestamp = 999L))

        val all = dao.getAllHistory().first()
        assertEquals("newer", all.first().title)
        assertEquals("older", all.last().title)
    }

    @Test
    fun dao_does_not_dedupe_same_url_at_dao_layer() = runTest {
        // DAO 层不负责去重，业务层（HistoryRepository.addHistory）才做：
        // 先 delete 旧的再 insert 新的。这里验证 DAO 本身的行为：直接两次 insert 会保留两条。
        dao.insert(HistoryEntity(title = "v1", url = "https://same.example"))
        dao.insert(HistoryEntity(title = "v2", url = "https://same.example"))

        val all = dao.getAllHistory().first()
        assertEquals(2, all.size)
        assertTrue(all.all { it.url == "https://same.example" })
    }
}
