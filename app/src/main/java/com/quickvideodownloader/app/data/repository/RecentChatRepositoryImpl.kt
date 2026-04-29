package com.quickvideodownloader.app.data.repository

import com.quickvideodownloader.app.data.local.dao.RecentChatDao
import com.quickvideodownloader.app.data.local.entity.RecentChatEntity
import com.quickvideodownloader.app.domain.repository.RecentChatRepository
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
