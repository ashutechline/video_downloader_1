package com.example.video_downloder.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.video_downloder.domain.model.Note

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Int? = null,
    val title: String,
    val content: String,
    val timestamp: Long
)
