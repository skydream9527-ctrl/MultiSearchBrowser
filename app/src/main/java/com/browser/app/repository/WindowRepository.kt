package com.browser.app.repository

import com.browser.app.data.dao.WindowDao
import com.browser.app.data.entity.WindowEntity
import kotlinx.coroutines.flow.Flow

class WindowRepository(private val windowDao: WindowDao) {
    fun getAllWindows(): Flow<List<WindowEntity>> = windowDao.getAllWindows()

    fun getCount(): Flow<Int> = windowDao.getCount()

    suspend fun addWindow(title: String, url: String): Long {
        return windowDao.insert(WindowEntity(title = title, url = url))
    }

    suspend fun updateWindow(window: WindowEntity) {
        windowDao.update(window)
    }

    suspend fun deleteWindow(window: WindowEntity) {
        windowDao.delete(window)
    }

    suspend fun deleteWindowById(id: Long) {
        windowDao.deleteById(id)
    }

    suspend fun getWindowById(id: Long): WindowEntity? {
        return windowDao.getById(id)
    }
}