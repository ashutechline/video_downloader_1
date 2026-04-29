package com.example.video_downloder.data.repository

import com.example.video_downloder.data.local.dao.RecentChatDao
import com.example.video_downloder.data.local.entity.RecentChatEntity
import com.example.video_downloder.domain.repository.RecentChatRepository
import kotlinx.coroutines.flow.Flow

class RecentChatRepositoryImpl(
    private val dao: RecentChatDao
) : RecentChatRepository {
    override fun getAllRecentChats(): Flow<List<RecentChatEntity>> = dao.getAllRecentChats()

    override suspend fun insertRecentChat(chat: RecentChatEntity) {
        dao.insertRecentChat(chat)
    }

    override suspend fun clearAllRecentChats() {
        dao.clearAllRecentChats()
    }
}
