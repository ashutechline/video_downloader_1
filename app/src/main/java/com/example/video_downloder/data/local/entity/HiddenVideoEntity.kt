package com.example.video_downloder.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "hidden_videos")
data class HiddenVideoEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val path: String,
    val originalPath: String,
    val duration: Long,
    val size: Long,
    val hiddenAt: Long = System.currentTimeMillis()
)
