package com.example.video_downloder.data.remote.dto

import com.google.gson.annotations.SerializedName

data class DownloadRequest(
    val url: String,
    val formatId: String,
    @SerializedName("format")
    val format: String
)

data class DownloadResponse(
    val success: Boolean,
    val downloadUrl: String?,
    val message: String?
)
