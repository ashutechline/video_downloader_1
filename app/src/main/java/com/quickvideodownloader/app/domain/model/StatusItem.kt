package com.quickvideodownloader.app.domain.model

import android.net.Uri

data class StatusItem(
    val id: String,
    val name: String,
    val uri: Uri,
    val fileType: FileType,
    val extension: String,
    val size: Long,
    val dateModified: Long,
    val duration: String? = null,
    var isSelected: Boolean = false,
    var isSaved: Boolean = false
)

enum class FileType {
    IMAGE,
    VIDEO
}
