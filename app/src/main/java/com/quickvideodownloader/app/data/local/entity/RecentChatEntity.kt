package com.quickvideodownloader.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recent_chats")
data class RecentChatEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val number: String,
    val message: String,
    val timestamp: Long
)
