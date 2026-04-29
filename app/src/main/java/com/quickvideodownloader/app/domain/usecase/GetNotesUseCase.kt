package com.quickvideodownloader.app.domain.usecase

import com.quickvideodownloader.app.domain.model.Note
import com.quickvideodownloader.app.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetNotesUseCase @Inject constructor(
    private val repository: NoteRepository
) {
    operator fun invoke(): Flow<List<Note>> {
        return repository.getNotes()
    }
}
