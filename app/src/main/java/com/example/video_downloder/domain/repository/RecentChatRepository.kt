package com.example.video_downloder.domain.repository

import com.example.video_downloder.data.local.entity.RecentChatEntity
import kotlinx.coroutines.flow.Flow

interface RecentChatRepository {
    fun getAllRecentChats(): Flow<List<RecentChatEntity>>
    suspend fun insertRecentChat(chat: RecentChatEntity)
    suspend fun clearAllRecentChats()
}
