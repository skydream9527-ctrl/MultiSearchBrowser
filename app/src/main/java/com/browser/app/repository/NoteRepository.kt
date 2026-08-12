package com.browser.app.repository

import com.browser.app.data.dao.NoteDao
import com.browser.app.data.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

class NoteRepository(private val dao: NoteDao) {
    fun getAllNotes(): Flow<List<NoteEntity>> = dao.getAllNotes()
    fun getNotesByUrl(url: String): Flow<List<NoteEntity>> = dao.getNotesByUrl(url)
    fun searchNotes(query: String): Flow<List<NoteEntity>> = dao.searchNotes(query)
    fun getCount(): Flow<Int> = dao.getCount()

    suspend fun addNote(text: String, sourceUrl: String = "", sourceTitle: String = ""): Long {
        return dao.insert(NoteEntity(text = text, sourceUrl = sourceUrl, sourceTitle = sourceTitle))
    }

    suspend fun delete(note: NoteEntity) = dao.delete(note)
    suspend fun deleteById(id: Long) = dao.deleteById(id)
    suspend fun deleteAll() = dao.deleteAll()
}
