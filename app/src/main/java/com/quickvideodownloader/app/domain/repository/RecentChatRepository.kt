package com.quickvideodownloader.app.domain.repository

import com.quickvideodownloader.app.data.local.entity.RecentChatEntity
import kotlinx.coroutines.flow.Flow

interface RecentChatRepository {
    fun getAllRecentChats(): Flow<List<RecentChatEntity>>
    suspend fun insertRecentChat(chat: RecentChatEntity)
    suspend fun clearAllRecentChats()
}
