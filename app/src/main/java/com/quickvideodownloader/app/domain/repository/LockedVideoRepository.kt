package com.quickvideodownloader.app.domain.repository

import com.quickvideodownloader.app.data.local.entity.LockedVideoEntity
import kotlinx.coroutines.flow.Flow

interface LockedVideoRepository {
    fun getAllLockedVideos(): Flow<List<LockedVideoEntity>>
    suspend fun lockVideo(video: LockedVideoEntity)
    suspend fun unlockVideo(video: LockedVideoEntity)
    fun getLockedVideosCount(): Flow<Int>
    suspend fun isVideoLocked(id: Long): Boolean
}
