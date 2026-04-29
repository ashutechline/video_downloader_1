package com.example.video_downloder.domain.repository

import com.example.video_downloder.data.remote.dto.DownloadResponse
import com.example.video_downloder.data.remote.dto.VideoInfoResponse

import kotlinx.coroutines.flow.Flow

sealed class DownloadStatus {
    data class Progress(val progress: Int, val downloadedBytes: Long, val totalBytes: Long) : DownloadStatus()
    data class Success(val filePath: String) : DownloadStatus()
    data class Error(val message: String) : DownloadStatus()
}

interface VideoRepository {
    suspend fun getVideoInfo(url: String): Result<VideoInfoResponse>
    fun downloadVideo(url: String, formatId: String, quality: String): Flow<DownloadStatus>
}
