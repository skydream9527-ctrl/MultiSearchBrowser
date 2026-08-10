package com.browser.app.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.browser.app.data.TestDb
import com.browser.app.data.entity.HistoryEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class HistoryRepositoryTest {

    private val db = TestDb.build()
    private val repo = HistoryRepository(db.historyDao())

    @After
    fun teardown() = db.close()

    @Test
    fun addHistory_then_getAllHistory_returns_item() = runTest {
        repo.addHistory(title = "Kotlin", url = "https://kotlinlang.org")

        val all = repo.getAllHistory().first()
        assertEquals(1, all.size)
        assertEquals("Kotlin", all.first().title)
    }

    @Test
    fun addHistory_dedupes_existing_url() = runTest {
        // Repository 在 DAO 之上做去重：先 delete 旧的同 url 记录再 insert 新的
        repo.addHistory(title = "v1", url = "https://same.example")
        repo.addHistory(title = "v2", url = "https://same.example")

        val all = repo.getAllHistory().first()
        assertEquals(1, all.size)
        assertEquals("v2", all.first().title)
    }

    @Test
    fun addHistory_does_not_dedupe_different_urls() = runTest {
        repo.addHistory(title = "a", url = "https://a.example")
        repo.addHistory(title = "b", url = "https://b.example")

        val all = repo.getAllHistory().first()
        assertEquals(2, all.size)
    }

    @Test
    fun deleteHistory_removes_specific_entity() = runTest {
        repo.addHistory(title = "x", url = "https://x.example")
        repo.addHistory(title = "y", url = "https://y.example")

        val persisted = repo.getAllHistory().first().first { it.title == "x" }
        repo.deleteHistory(persisted)

        val all = repo.getAllHistory().first()
        assertEquals(1, all.size)
        assertEquals("https://y.example", all.first().url)
    }

    @Test
    fun clearHistory_empties_table_and_resets_count() = runTest {
        repo.addHistory(title = "1", url = "https://1.example")
        repo.addHistory(title = "2", url = "https://2.example")
        repo.addHistory(title = "3", url = "https://3.example")

        repo.clearHistory()

        assertEquals(0, repo.getAllHistory().first().size)
        assertEquals(0, repo.getCount().first())
    }

    @Test
    fun getCount_reflects_repository_dedup() = runTest {
        repo.addHistory(title = "v1", url = "https://same.example")
        repo.addHistory(title = "v2", url = "https://same.example")
        repo.addHistory(title = "different", url = "https://other.example")

        // 两行被 dedup 合成一行 + 一行不同 url = 2
        assertEquals(2, repo.getCount().first())
    }

    @Test
    fun deleteHistory_with_unsaved_entity_is_no_op() = runTest {
        repo.addHistory(title = "keep", url = "https://keep.example")

        // 没存过的 entity 直接传给 delete：DAO 走主键匹配，找不到就是 no-op
        repo.deleteHistory(HistoryEntity(title = "ghost", url = "https://ghost.example"))

        assertEquals(1, repo.getAllHistory().first().size)
    }
}
