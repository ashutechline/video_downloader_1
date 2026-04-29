package com.quickvideodownloader.app.domain.repository

import com.quickvideodownloader.app.domain.model.VideoItem
import kotlinx.coroutines.flow.Flow

interface MediaRepository {
    fun getAllVideos(): Flow<List<VideoItem>>
    suspend fun toggleLock(mediaId: Long)
    suspend fun toggleHide(mediaId: Long)
}
