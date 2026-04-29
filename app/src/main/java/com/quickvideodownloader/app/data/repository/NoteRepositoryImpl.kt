package com.quickvideodownloader.app.data.repository

import com.quickvideodownloader.app.data.local.dao.NoteDao
import com.quickvideodownloader.app.data.mapper.toNote
import com.quickvideodownloader.app.data.mapper.toNoteEntity
import com.quickvideodownloader.app.domain.model.Note
import com.quickvideodownloader.app.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class NoteRepositoryImpl(
    private val dao: NoteDao
) : NoteRepository {
    override fun getNotes(): Flow<List<Note>> {
        return dao.getNotes().map { entities ->
            entities.map { it.toNote() }
        }
    }

    override suspend fun getNoteById(id: Int): Note? {
        return dao.getNoteById(id)?.toNote()
    }

    override suspend fun insertNote(note: Note) {
        dao.insertNote(note.toNoteEntity())
    }

    override suspend fun deleteNote(note: Note) {
        dao.deleteNote(note.toNoteEntity())
    }
}
