package com.example.video_downloder.data.remote.api

import com.example.video_downloder.data.remote.dto.DownloadRequest
import com.example.video_downloder.data.remote.dto.DownloadResponse
import com.example.video_downloder.data.remote.dto.VideoInfoRequest
import com.example.video_downloder.data.remote.dto.VideoInfoResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Streaming

interface VideoApi {
    @POST("api/video-info")
    suspend fun getVideoInfo(
        @Body request: VideoInfoRequest
    ): VideoInfoResponse

    @POST("api/download")
    suspend fun getDownloadUrl(
        @Body request: DownloadRequest
    ): com.example.video_downloder.data.remote.dto.DownloadResponse

    @Streaming
    @POST("api/download")
    suspend fun downloadVideo(
        @Body request: DownloadRequest
    ): Response<ResponseBody>
}
