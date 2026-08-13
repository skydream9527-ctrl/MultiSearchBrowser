package com.browser.app.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.browser.app.data.BrowserDatabase
import com.browser.app.data.entity.RssEntity
import com.browser.app.data.entity.RssItemEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * RssRepository 单元测试（Robolectric 内存数据库）。
 * 验证 addFeed / getAllFeeds / getEnabledFeeds / deleteFeed / deleteFeedById /
 * updateLastFetched / insertItem / markAsRead / getUnreadCount / deleteOldItems 等 CRUD。
 */
@RunWith(RobolectricTestRunner::class)
class RssRepositoryTest {

    private lateinit var database: BrowserDatabase
    private lateinit var repo: RssRepository

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            BrowserDatabase::class.java
        ).allowMainThreadQueries().build()
        repo = RssRepository(database.rssDao())
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun getAllFeeds_emptyInitially() = runTest {
        assertTrue(repo.getAllFeeds().first().isEmpty())
        assertTrue(repo.getEnabledFeeds().first().isEmpty())
    }

    @Test
    fun addFeed_andRetrieve() = runTest {
        val id = repo.addFeed(name = "Hacker News", url = "https://hnrss.org/frontpage")

        assertTrue("addFeed 应返回有效 id", id > 0)
        val all = repo.getAllFeeds().first()
        assertEquals(1, all.size)
        assertEquals("Hacker News", all[0].name)
        assertEquals("https://hnrss.org/frontpage", all[0].url)
        // 新增 feed 默认 enabled = true
        assertTrue(all[0].enabled)
        // 默认 lastFetched = 0
        assertEquals(0L, all[0].lastFetched)
    }

    @Test
    fun getEnabledFeeds_filtersDisabled() = runTest {
        // Repository 无 setEnabled API，通过 DAO 直接插入一个 enabled=false 的 feed
        val dao = database.rssDao()
        dao.insertFeed(RssEntity(name = "Active", url = "https://a.com", enabled = true))
        dao.insertFeed(RssEntity(name = "Disabled", url = "https://b.com", enabled = false))

        val enabled = repo.getEnabledFeeds().first()
        assertEquals(1, enabled.size)
        assertEquals("Active", enabled[0].name)

        val all = repo.getAllFeeds().first()
        assertEquals(2, all.size)
    }

    @Test
    fun deleteFeed_removesEntry() = runTest {
        repo.addFeed("ToDelete", "https://delete.com")
        val feed = repo.getAllFeeds().first()[0]

        repo.deleteFeed(feed)

        assertTrue(repo.getAllFeeds().first().isEmpty())
    }

    @Test
    fun deleteFeedById_removesEntry() = runTest {
        val id = repo.addFeed("ById", "https://byid.com")
        repo.deleteFeedById(id)

        assertTrue(repo.getAllFeeds().first().isEmpty())
    }

    @Test
    fun updateLastFetched_persistsTimestamp() = runTest {
        val id = repo.addFeed("Fetch", "https://fetch.com")
        val ts = System.currentTimeMillis()
        repo.updateLastFetched(id, ts)

        val feed = repo.getAllFeeds().first()[0]
        assertEquals(ts, feed.lastFetched)
    }

    @Test
    fun insertItem_andRetrieve() = runTest {
        val feedId = repo.addFeed("Feed", "https://feed.com")
        repo.insertItem(
            RssItemEntity(
                feedId = feedId,
                guid = "guid-1",
                title = "Item 1",
                link = "https://feed.com/1",
                description = "desc",
                pubDate = System.currentTimeMillis()
            )
        )

        val items = repo.getAllItems().first()
        assertEquals(1, items.size)
        assertEquals("Item 1", items[0].title)
        assertEquals("guid-1", items[0].guid)
        assertFalse("新条目默认未读", items[0].isRead)
    }

    @Test
    fun markAsRead_setsReadFlag() = runTest {
        val feedId = repo.addFeed("Feed", "https://feed.com")
        repo.insertItem(
            RssItemEntity(feedId = feedId, guid = "g1", title = "T", link = "L")
        )

        repo.markAsRead("g1")

        val items = repo.getAllItems().first()
        assertTrue(items[0].isRead)
        // 未读计数应为 0
        assertEquals(0, repo.getUnreadCount().first())
    }

    @Test
    fun getUnreadCount_tracksUnread() = runTest {
        val feedId = repo.addFeed("Feed", "https://feed.com")
        repo.insertItem(RssItemEntity(feedId = feedId, guid = "g1", title = "1", link = "l1"))
        repo.insertItem(RssItemEntity(feedId = feedId, guid = "g2", title = "2", link = "l2"))
        repo.insertItem(RssItemEntity(feedId = feedId, guid = "g3", title = "3", link = "l3"))

        assertEquals(3, repo.getUnreadCount().first())

        repo.markAsRead("g1")
        assertEquals(2, repo.getUnreadCount().first())

        repo.markAsRead("g2")
        repo.markAsRead("g3")
        assertEquals(0, repo.getUnreadCount().first())
    }

    @Test
    fun deleteOldItems_removesOlderThanThreshold() = runTest {
        val feedId = repo.addFeed("Feed", "https://feed.com")
        val now = System.currentTimeMillis()
        repo.insertItem(
            RssItemEntity(
                feedId = feedId, guid = "old", title = "old", link = "l", pubDate = now - 10000
            )
        )
        repo.insertItem(
            RssItemEntity(
                feedId = feedId, guid = "new", title = "new", link = "l", pubDate = now
            )
        )

        // 删除 pubDate < (now - 5000) 的条目
        repo.deleteOldItems(now - 5000)

        val items = repo.getAllItems().first()
        assertEquals(1, items.size)
        assertEquals("new", items[0].title)
    }
}
