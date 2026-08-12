package com.browser.app.repository

import com.browser.app.data.dao.RssDao
import com.browser.app.data.entity.RssEntity
import com.browser.app.data.entity.RssItemEntity
import kotlinx.coroutines.flow.Flow

class RssRepository(private val dao: RssDao) {
    fun getEnabledFeeds(): Flow<List<RssEntity>> = dao.getEnabledFeeds()
    fun getAllFeeds(): Flow<List<RssEntity>> = dao.getAllFeeds()
    fun getAllItems(): Flow<List<RssItemEntity>> = dao.getAllItems()
    fun getUnreadCount(): Flow<Int> = dao.getUnreadCount()

    suspend fun addFeed(name: String, url: String): Long {
        return dao.insertFeed(RssEntity(name = name, url = url))
    }

    suspend fun deleteFeed(feed: RssEntity) = dao.deleteFeed(feed)
    suspend fun deleteFeedById(id: Long) = dao.deleteFeedById(id)
    suspend fun updateLastFetched(id: Long, timestamp: Long) = dao.updateLastFetched(id, timestamp)
    suspend fun insertItem(item: RssItemEntity) = dao.insertItem(item)
    suspend fun markAsRead(guid: String) = dao.markAsRead(guid)
    suspend fun deleteOldItems(before: Long) = dao.deleteOldItems(before)
}
