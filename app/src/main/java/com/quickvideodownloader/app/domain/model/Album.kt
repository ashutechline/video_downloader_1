package com.quickvideodownloader.app.domain.model

data class Album(
    val name: String,
    val id: String,
    val videos: List<VideoItem>,
    val thumbnail: String
)
