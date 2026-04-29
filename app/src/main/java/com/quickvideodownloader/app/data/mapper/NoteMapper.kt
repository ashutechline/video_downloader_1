package com.quickvideodownloader.app.data.mapper

import com.quickvideodownloader.app.data.local.entity.NoteEntity
import com.quickvideodownloader.app.domain.model.Note

fun NoteEntity.toNote(): Note {
    return Note(
        id = id,
        title = title,
        content = content,
        timestamp = timestamp
    )
}

fun Note.toNoteEntity(): NoteEntity {
    return NoteEntity(
        id = id,
        title = title,
        content = content,
        timestamp = timestamp
    )
}
