package com.quickvideodownloader.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.quickvideodownloader.app.domain.model.DownloadItem

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey
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
    val thumbnail: String? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toDomainModel(): DownloadItem {
        return DownloadItem(
            id = id,
            name = name,
            url = url,
            downloadUrl = downloadUrl,
            formatId = formatId,
            quality = quality,
            progress = progress,
            downloadedSize = downloadedSize,
            totalSize = totalSize,
            speed = speed,
            isCompleted = isCompleted,
            isPaused = isPaused,
            filePath = filePath,
            thumbnail = thumbnail
        )
    }

    companion object {
        fun fromDomainModel(item: DownloadItem): DownloadEntity {
            return DownloadEntity(
                id = item.id,
                name = item.name,
                url = item.url,
                downloadUrl = item.downloadUrl,
                formatId = item.formatId,
                quality = item.quality,
                progress = item.progress,
                downloadedSize = item.downloadedSize,
                totalSize = item.totalSize,
                speed = item.speed,
                isCompleted = item.isCompleted,
                isPaused = item.isPaused,
                filePath = item.filePath,
                thumbnail = item.thumbnail
            )
        }
    }
}
