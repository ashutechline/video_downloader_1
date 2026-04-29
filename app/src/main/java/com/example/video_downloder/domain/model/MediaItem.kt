package com.example.video_downloder.domain.model

data class MediaItem(
    val id: Long,
    val name: String,
    val path: String,
    val duration: String,
    val isVideo: Boolean,
    val isLocked: Boolean = false,
    val isHidden: Boolean = false,
    val thumbnail: String = ""
)
