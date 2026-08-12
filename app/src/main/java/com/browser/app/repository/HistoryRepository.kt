package com.browser.app.repository

import com.browser.app.data.dao.HistoryDao
import com.browser.app.data.entity.HistoryEntity
import kotlinx.coroutines.flow.Flow

class HistoryRepository(private val historyDao: HistoryDao) {
    fun getAllHistory(): Flow<List<HistoryEntity>> = historyDao.getAllHistory()

    suspend fun addHistory(title: String, url: String) {
        historyDao.upsert(title, url)
    }

    suspend fun deleteHistory(history: HistoryEntity) = historyDao.delete(history)

    suspend fun clearHistory() = historyDao.deleteAll()

    fun getCount(): Flow<Int> = historyDao.getCount()
}