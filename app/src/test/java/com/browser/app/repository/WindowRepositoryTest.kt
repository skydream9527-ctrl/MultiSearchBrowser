package com.browser.app.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.browser.app.data.TestDb
import com.browser.app.data.entity.WindowEntity
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
class WindowRepositoryTest {

    private val db = TestDb.build()
    private val repo = WindowRepository(db.windowDao())

    @After
    fun teardown() = db.close()

    @Test
    fun addWindow_returns_generated_id_and_persists_entity() = runTest {
        val id = repo.addWindow(title = "Tab1", url = "https://tab1.example")

        assertTrue(id > 0L)
        val found = repo.getWindowById(id)
        assertNotNull(found)
        assertEquals("Tab1", found?.title)
    }

    @Test
    fun getAllWindows_returns_all_inserted() = runTest {
        repo.addWindow(title = "a", url = "https://a.example")
        repo.addWindow(title = "b", url = "https://b.example")

        val all = repo.getAllWindows().first()
        assertEquals(2, all.size)
    }

    @Test
    fun getCount_reflects_inserts_and_deletes() = runTest {
        assertEquals(0, repo.getCount().first())

        val id1 = repo.addWindow(title = "1", url = "https://1.example")
        val id2 = repo.addWindow(title = "2", url = "https://2.example")
        assertEquals(2, repo.getCount().first())

        repo.deleteWindowById(id1)
        assertEquals(1, repo.getCount().first())

        repo.deleteWindowById(id2)
        assertEquals(0, repo.getCount().first())
    }

    @Test
    fun updateWindow_overrides_existing_fields() = runTest {
        val id = repo.addWindow(title = "Old", url = "https://old.example")

        val persisted = repo.getWindowById(id)!!
        repo.updateWindow(persisted.copy(title = "New", url = "https://new.example"))

        val updated = repo.getWindowById(id)!!
        assertEquals("New", updated.title)
        assertEquals("https://new.example", updated.url)
    }

    @Test
    fun deleteWindow_entity_removes_row() = runTest {
        val id = repo.addWindow(title = "X", url = "https://x.example")
        val persisted = repo.getWindowById(id)!!

        repo.deleteWindow(persisted)

        assertNull(repo.getWindowById(id))
    }

    @Test
    fun deleteWindowById_removes_target_only() = runTest {
        val keepId = repo.addWindow(title = "Keep", url = "https://keep.example")
        val dropId = repo.addWindow(title = "Drop", url = "https://drop.example")

        repo.deleteWindowById(dropId)

        assertNull(repo.getWindowById(dropId))
        assertNotNull(repo.getWindowById(keepId))
    }

    @Test
    fun getWindowById_returns_null_when_not_exists() = runTest {
        assertNull(repo.getWindowById(99999L))
    }

    @Test
    fun deleteWindow_with_unsaved_entity_is_no_op() = runTest {
        repo.addWindow(title = "keep", url = "https://keep.example")

        // 传入未持久化的 entity：DAO 按主键匹配，找不到不报错
        repo.deleteWindow(WindowEntity(title = "ghost", url = "https://ghost.example"))

        assertEquals(1, repo.getAllWindows().first().size)
    }
}
