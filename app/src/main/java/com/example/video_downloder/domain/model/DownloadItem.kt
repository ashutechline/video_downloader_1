package com.example.video_downloder.domain.model

data class DownloadItem(
    val id: String,
    val name: String,
    val url: String,
    val downloadUrl: String? = null,
    val formatId: String,
    val quality: String,
    val progress: Int,
    val downloadedSize: Long,
    val totalSize: Long,
    val speed: String,
    val isCompleted: Boolean,
    val isPaused: Boolean = false,
    val filePath: String,
    val thumbnail: String? = null
)

