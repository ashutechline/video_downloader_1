package com.example.video_downloder.presentation.state

import com.example.video_downloder.domain.model.Note

data class NoteState(
    val notes: List<Note> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
