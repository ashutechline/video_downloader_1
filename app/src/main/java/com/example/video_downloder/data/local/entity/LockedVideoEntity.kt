package com.example.video_downloder.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "locked_videos")
data class LockedVideoEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val path: String,
    val duration: Long,
    val size: Long,
    val lockedAt: Long = System.currentTimeMillis()
)
