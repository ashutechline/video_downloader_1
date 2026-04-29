package com.example.video_downloder.domain.repository

import com.example.video_downloder.data.local.entity.HiddenVideoEntity
import kotlinx.coroutines.flow.Flow

interface HiddenVideoRepository {
    fun getAllHiddenVideos(): Flow<List<HiddenVideoEntity>>
    suspend fun hideVideo(video: HiddenVideoEntity)
    suspend fun unhideVideo(video: HiddenVideoEntity)
    suspend fun deleteVideo(video: HiddenVideoEntity)
    fun getHiddenVideosCount(): Flow<Int>
    suspend fun getAllHiddenVideosList(): List<HiddenVideoEntity>
}
