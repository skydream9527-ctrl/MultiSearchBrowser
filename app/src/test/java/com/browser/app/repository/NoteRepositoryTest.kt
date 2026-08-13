package com.browser.app.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.browser.app.data.BrowserDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * NoteRepository 单元测试（Robolectric 内存数据库）。
 * 验证 addNote / getAllNotes / delete / deleteById / deleteAll / getCount 等 API。
 */
@RunWith(RobolectricTestRunner::class)
class NoteRepositoryTest {

    private lateinit var database: BrowserDatabase
    private lateinit var repo: NoteRepository

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            BrowserDatabase::class.java
        ).allowMainThreadQueries().build()
        repo = NoteRepository(database.noteDao())
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun getAllNotes_emptyInitially() = runTest {
        assertTrue(repo.getAllNotes().first().isEmpty())
        assertEquals(0, repo.getCount().first())
    }

    @Test
    fun addNote_andRetrieve() = runTest {
        val id = repo.addNote(
            text = "第一条笔记",
            sourceUrl = "https://example.com",
            sourceTitle = "Example"
        )

        assertTrue("addNote 应返回有效 id", id > 0)
        val all = repo.getAllNotes().first()
        assertEquals(1, all.size)
        assertEquals("第一条笔记", all[0].text)
        assertEquals("https://example.com", all[0].sourceUrl)
        assertEquals("Example", all[0].sourceTitle)
    }

    @Test
    fun addNote_withDefaults_emptySource() = runTest {
        repo.addNote(text = "纯文本笔记")

        val note = repo.getAllNotes().first()[0]
        assertEquals("纯文本笔记", note.text)
        assertEquals("", note.sourceUrl)
        assertEquals("", note.sourceTitle)
    }

    @Test
    fun addMultipleNotes_allReturned() = runTest {
        repo.addNote("A")
        repo.addNote("B")
        repo.addNote("C")

        val all = repo.getAllNotes().first()
        assertEquals(3, all.size)
        val texts = all.map { it.text }.toSet()
        assertTrue("A" in texts)
        assertTrue("B" in texts)
        assertTrue("C" in texts)
    }

    @Test
    fun delete_removesNote() = runTest {
        repo.addNote("待删除")
        val note = repo.getAllNotes().first()[0]

        repo.delete(note)

        assertTrue(repo.getAllNotes().first().isEmpty())
    }

    @Test
    fun deleteById_removesNote() = runTest {
        val id = repo.addNote("by id")
        assertEquals(1, repo.getAllNotes().first().size)

        repo.deleteById(id)

        assertTrue(repo.getAllNotes().first().isEmpty())
    }

    @Test
    fun deleteAll_clearsAllNotes() = runTest {
        repo.addNote("A")
        repo.addNote("B")
        repo.addNote("C")
        assertEquals(3, repo.getCount().first())

        repo.deleteAll()

        assertEquals(0, repo.getCount().first())
        assertTrue(repo.getAllNotes().first().isEmpty())
    }

    @Test
    fun getCount_tracksSize() = runTest {
        assertEquals(0, repo.getCount().first())
        repo.addNote("one")
        assertEquals(1, repo.getCount().first())
        repo.addNote("two")
        assertEquals(2, repo.getCount().first())
    }
}
