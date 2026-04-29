package com.example.video_downloder.data.local.dao

import androidx.room.*
import com.example.video_downloder.data.local.entity.RecentChatEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentChatDao {
    @Query("SELECT * FROM recent_chats ORDER BY timestamp DESC")
    fun getAllRecentChats(): Flow<List<RecentChatEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecentChat(chat: RecentChatEntity)

    @Query("DELETE FROM recent_chats")
    suspend fun clearAllRecentChats()

    @Query("DELETE FROM recent_chats WHERE id = :id")
    suspend fun deleteRecentChat(id: Int)
}
