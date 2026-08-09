package com.browser.app.repository

import com.browser.app.data.dao.HistoryDao
import com.browser.app.data.entity.HistoryEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryRepository @Inject constructor(private val historyDao: HistoryDao) {
    fun getAllHistory(): Flow<List<HistoryEntity>> = historyDao.getAllHistory()

    suspend fun addHistory(title: String, url: String) {
        val existing = historyDao.getByUrl(url)
        if (existing != null) {
            historyDao.delete(existing)
        }
        historyDao.insert(HistoryEntity(title = title, url = url))
    }

    suspend fun deleteHistory(history: HistoryEntity) = historyDao.delete(history)

    suspend fun clearHistory() = historyDao.deleteAll()

    fun getCount(): Flow<Int> = historyDao.getCount()
}
