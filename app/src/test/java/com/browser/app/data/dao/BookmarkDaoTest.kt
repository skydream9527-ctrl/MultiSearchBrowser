package com.browser.app.data.dao

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.browser.app.data.TestDb
import com.browser.app.data.entity.BookmarkEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class BookmarkDaoTest {

    private val db = TestDb.build()
    private val dao: BookmarkDao = db.bookmarkDao()

    @After
    fun teardown() = db.close()

    @Test
    fun insert_then_getAllBookmarks_returns_one_item() = runTest {
        dao.insert(BookmarkEntity(title = "Google", url = "https://google.com"))

        val all = dao.getAllBookmarks().first()
        assertEquals(1, all.size)
        assertEquals("https://google.com", all.first().url)
    }

    @Test
    fun insert_with_same_url_does_not_replace_when_url_not_unique() = runTest {
        // BookmarkEntity 的主键是自增 id，url 列没有 unique 约束，
        // 因此 OnConflictStrategy.REPLACE 不会在 url 冲突时触发，
        // 两次 insert 会产生两条 url 相同的记录（业务层去重交给 Repository.toggleBookmark 处理）。
        dao.insert(BookmarkEntity(title = "Old", url = "https://example.com"))
        dao.insert(BookmarkEntity(title = "New", url = "https://example.com"))

        val all = dao.getAllBookmarks().first()
        assertEquals(2, all.size)
        assertTrue(all.map { it.url }.all { it == "https://example.com" })
    }

    @Test
    fun getByUrl_returns_null_when_not_exists() = runTest {
        assertNull(dao.getByUrl("https://nope.example"))
    }

    @Test
    fun isBookmarked_emits_true_after_insert_and_false_after_delete() = runTest {
        val url = "https://github.com"
        assertFalse(dao.isBookmarked(url).first())

        dao.insert(BookmarkEntity(title = "GitHub", url = url))
        assertTrue(dao.isBookmarked(url).first())

        dao.deleteByUrl(url)
        assertFalse(dao.isBookmarked(url).first())
    }

    @Test
    fun deleteByUrl_removes_only_matching_url() = runTest {
        dao.insert(BookmarkEntity(title = "A", url = "https://a.example"))
        dao.insert(BookmarkEntity(title = "B", url = "https://b.example"))

        dao.deleteByUrl("https://a.example")

        val all = dao.getAllBookmarks().first()
        assertEquals(1, all.size)
        assertEquals("https://b.example", all.first().url)
    }

    @Test
    fun delete_entity_removes_exact_row() = runTest {
        val bookmark = BookmarkEntity(title = "ToDelete", url = "https://delete.example")
        dao.insert(bookmark)
        val inserted = dao.getByUrl("https://delete.example")!!

        dao.delete(inserted)

        assertEquals(0, dao.getAllBookmarks().first().size)
    }

    @Test
    fun getAllBookmarks_ordered_by_timestamp_desc() = runTest {
        dao.insert(BookmarkEntity(title = "old", url = "https://old.example", timestamp = 100L))
        dao.insert(BookmarkEntity(title = "new", url = "https://new.example", timestamp = 1000L))

        val all = dao.getAllBookmarks().first()
        assertEquals("new", all.first().title)
        assertEquals("old", all.last().title)
    }

    @Test
    fun faviconUrl_can_be_persisted_as_nullable() = runTest {
        dao.insert(
            BookmarkEntity(
                title = "WithFavicon",
                url = "https://fav.example",
                faviconUrl = "https://fav.example/favicon.ico"
            )
        )
        dao.insert(
            BookmarkEntity(title = "NoFavicon", url = "https://nofav.example", faviconUrl = null)
        )

        val withFav = dao.getByUrl("https://fav.example")
        val noFav = dao.getByUrl("https://nofav.example")

        assertEquals("https://fav.example/favicon.ico", withFav?.faviconUrl)
        assertNull(noFav?.faviconUrl)
    }
}
