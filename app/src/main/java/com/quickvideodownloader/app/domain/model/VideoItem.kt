package com.quickvideodownloader.app.domain.model

data class VideoItem(
    val id: Long,
    val name: String,
    val path: String,
    val albumName: String,
    val albumId: String,
    val duration: Long,
    val isLocked: Boolean = false,
    val isHidden: Boolean = false,
    val thumbnail: String = ""
)
