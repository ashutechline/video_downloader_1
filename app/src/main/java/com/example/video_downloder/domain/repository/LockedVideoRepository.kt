package com.example.video_downloder.domain.repository

import com.example.video_downloder.data.local.entity.LockedVideoEntity
import kotlinx.coroutines.flow.Flow

interface LockedVideoRepository {
    fun getAllLockedVideos(): Flow<List<LockedVideoEntity>>
    suspend fun lockVideo(video: LockedVideoEntity)
    suspend fun unlockVideo(video: LockedVideoEntity)
    fun getLockedVideosCount(): Flow<Int>
    suspend fun isVideoLocked(id: Long): Boolean
}
