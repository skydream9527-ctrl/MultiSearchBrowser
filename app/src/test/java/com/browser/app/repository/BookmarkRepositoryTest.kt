package com.browser.app.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.browser.app.data.TestDb
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class BookmarkRepositoryTest {

    private val db = TestDb.build()
    private val repo = BookmarkRepository(db.bookmarkDao())

    @After
    fun teardown() = db.close()

    @Test
    fun addBookmark_then_getAllBookmarks_returns_item() = runTest {
        repo.addBookmark(title = "Google", url = "https://google.com")

        val all = repo.getAllBookmarks().first()
        assertEquals(1, all.size)
        assertEquals("Google", all.first().title)
    }

    @Test
    fun isBookmarked_reflects_repository_state() = runTest {
        assertFalse(repo.isBookmarked("https://x.example").first())

        repo.addBookmark(title = "X", url = "https://x.example")
        assertTrue(repo.isBookmarked("https://x.example").first())
    }

    @Test
    fun removeBookmark_clears_url() = runTest {
        repo.addBookmark(title = "Y", url = "https://y.example")

        repo.removeBookmark("https://y.example")

        assertEquals(0, repo.getAllBookmarks().first().size)
        assertFalse(repo.isBookmarked("https://y.example").first())
    }

    @Test
    fun toggleBookmark_adds_when_absent_returns_true() = runTest {
        val added = repo.toggleBookmark(title = "T1", url = "https://t1.example")

        assertTrue(added)
        assertTrue(repo.isBookmarked("https://t1.example").first())
        assertEquals(1, repo.getAllBookmarks().first().size)
    }

    @Test
    fun toggleBookmark_removes_when_present_returns_false() = runTest {
        repo.toggleBookmark(title = "T2", url = "https://t2.example")

        val removed = repo.toggleBookmark(title = "T2", url = "https://t2.example")

        assertFalse(removed)
        assertFalse(repo.isBookmarked("https://t2.example").first())
        assertEquals(0, repo.getAllBookmarks().first().size)
    }

    @Test
    fun toggleBookmark_does_not_affect_other_urls() = runTest {
        repo.toggleBookmark(title = "A", url = "https://a.example")
        repo.toggleBookmark(title = "B", url = "https://b.example")

        repo.toggleBookmark(title = "A", url = "https://a.example")

        val all = repo.getAllBookmarks().first()
        assertEquals(1, all.size)
        assertEquals("https://b.example", all.first().url)
    }
}
