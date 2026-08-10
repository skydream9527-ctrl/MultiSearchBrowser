package com.browser.app.data.dao

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
class WindowDaoTest {

    private val db = TestDb.build()
    private val dao: WindowDao = db.windowDao()

    @After
    fun teardown() = db.close()

    @Test
    fun insert_returns_generated_id() = runTest {
        val id = dao.insert(WindowEntity(title = "Tab1", url = "https://tab1.example"))

        assertTrue("自增 id 应大于 0", id > 0L)
    }

    @Test
    fun getById_returns_window_when_exists() = runTest {
        val id = dao.insert(WindowEntity(title = "Tab", url = "https://tab.example"))

        val found = dao.getById(id)
        assertNotNull(found)
        assertEquals("Tab", found?.title)
        assertEquals(id, found?.id)
    }

    @Test
    fun getById_returns_null_when_not_exists() = runTest {
        assertNull(dao.getById(9999L))
    }

    @Test
    fun update_overrides_existing_fields() = runTest {
        val id = dao.insert(WindowEntity(title = "Old", url = "https://old.example"))

        val persisted = dao.getById(id)!!
        dao.update(persisted.copy(title = "New", url = "https://new.example"))

        val updated = dao.getById(id)!!
        assertEquals("New", updated.title)
        assertEquals("https://new.example", updated.url)
    }

    @Test
    fun delete_entity_removes_row() = runTest {
        val id = dao.insert(WindowEntity(title = "X", url = "https://x.example"))
        val persisted = dao.getById(id)!!

        dao.delete(persisted)

        assertNull(dao.getById(id))
    }

    @Test
    fun deleteById_removes_only_target() = runTest {
        val keepId = dao.insert(WindowEntity(title = "Keep", url = "https://keep.example"))
        val dropId = dao.insert(WindowEntity(title = "Drop", url = "https://drop.example"))

        dao.deleteById(dropId)

        assertNull(dao.getById(dropId))
        assertNotNull(dao.getById(keepId))
    }

    @Test
    fun getCount_emits_zero_initially_then_reflects_inserts() = runTest {
        assertEquals(0, dao.getCount().first())

        dao.insert(WindowEntity(title = "a", url = "https://a.example"))
        dao.insert(WindowEntity(title = "b", url = "https://b.example"))

        assertEquals(2, dao.getCount().first())
    }

    @Test
    fun getAllWindows_returns_all_inserted_ordered_by_timestamp_desc() = runTest {
        dao.insert(WindowEntity(title = "older", url = "https://old.example", timestamp = 100L))
        dao.insert(WindowEntity(title = "newer", url = "https://new.example", timestamp = 500L))

        val all = dao.getAllWindows().first()
        assertEquals(2, all.size)
        assertEquals("newer", all.first().title)
        assertEquals("older", all.last().title)
    }
}
