package com.quickvideodownloader.app.presentation.state

import com.quickvideodownloader.app.domain.model.Note

data class NoteState(
    val notes: List<Note> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
