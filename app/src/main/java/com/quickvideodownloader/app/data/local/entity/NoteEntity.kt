package com.quickvideodownloader.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.quickvideodownloader.app.domain.model.Note

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Int? = null,
    val title: String,
    val content: String,
    val timestamp: Long
)
