package com.browser.app.data.dao

import androidx.room.*
import com.browser.app.data.entity.RssEntity
import com.browser.app.data.entity.RssItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RssDao {
    // Feed 管理
    @Query("SELECT * FROM rss_feeds WHERE enabled = 1 ORDER BY name")
    fun getEnabledFeeds(): Flow<List<RssEntity>>

    @Query("SELECT * FROM rss_feeds ORDER BY name")
    fun getAllFeeds(): Flow<List<RssEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeed(feed: RssEntity): Long

    @Update
    suspend fun updateFeed(feed: RssEntity)

    @Delete
    suspend fun deleteFeed(feed: RssEntity)

    @Query("DELETE FROM rss_feeds WHERE id = :id")
    suspend fun deleteFeedById(id: Long)

    @Query("UPDATE rss_feeds SET lastFetched = :timestamp WHERE id = :id")
    suspend fun updateLastFetched(id: Long, timestamp: Long)

    // Item 管理
    @Query("SELECT * FROM rss_items ORDER BY pubDate DESC LIMIT 100")
    fun getAllItems(): Flow<List<RssItemEntity>>

    @Query("SELECT * FROM rss_items WHERE feedId = :feedId ORDER BY pubDate DESC")
    fun getItemsByFeed(feedId: Long): Flow<List<RssItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: RssItemEntity): Long

    @Query("UPDATE rss_items SET isRead = 1 WHERE guid = :guid")
    suspend fun markAsRead(guid: String)

    @Query("SELECT COUNT(*) FROM rss_items WHERE isRead = 0")
    fun getUnreadCount(): Flow<Int>

    @Query("DELETE FROM rss_items WHERE feedId = :feedId")
    suspend fun deleteItemsByFeed(feedId: Long)

    @Query("DELETE FROM rss_items WHERE pubDate < :before")
    suspend fun deleteOldItems(before: Long)
}
