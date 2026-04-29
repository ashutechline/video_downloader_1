package com.example.video_downloder.domain.repository

import com.example.video_downloder.domain.model.VideoItem
import kotlinx.coroutines.flow.Flow

interface MediaRepository {
    fun getAllVideos(): Flow<List<VideoItem>>
    suspend fun toggleLock(mediaId: Long)
    suspend fun toggleHide(mediaId: Long)
}
