package com.quickvideodownloader.app.data.remote.dto

data class VideoInfoResponse(
    val success: Boolean,
    val title: String?,
    val thumbnail: String?,
    val duration: Double?,
    val formats: List<VideoFormatDto>?
)

data class VideoFormatDto(
    val quality: String?,
    val formatId: String?,
    val url: String?,
    val hasAudio: Boolean?
)
