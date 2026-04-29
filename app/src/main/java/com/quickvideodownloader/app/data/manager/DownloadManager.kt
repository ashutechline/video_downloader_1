package com.quickvideodownloader.app.data.manager

import android.content.Context
import android.content.Intent
import com.quickvideodownloader.app.data.local.dao.DownloadDao
import com.quickvideodownloader.app.data.local.entity.DownloadEntity
import com.quickvideodownloader.app.domain.model.DownloadItem
import com.quickvideodownloader.app.service.DownloadService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloadDao: DownloadDao
) {
    val downloads: Flow<List<DownloadItem>> = downloadDao.getAllDownloads().map { entities ->
        entities.map { it.toDomainModel() }
    }

    fun startDownload(
        url: String,
        formatId: String,
        quality: String,
        title: String,
        thumbnail: String?,
        directUrl: String? = null
    ): String {
        val id = UUID.randomUUID().toString()
        val intent = Intent(context, DownloadService::class.java).apply {
            action = DownloadService.ACTION_START_DOWNLOAD
            putExtra(DownloadService.EXTRA_ID, id)
            putExtra(DownloadService.EXTRA_URL, url)
            putExtra(DownloadService.EXTRA_FORMAT_ID, formatId)
            putExtra(DownloadService.EXTRA_QUALITY, quality)
            putExtra(DownloadService.EXTRA_TITLE, title)
            putExtra(DownloadService.EXTRA_THUMBNAIL, thumbnail)
            putExtra(DownloadService.EXTRA_DIRECT_URL, directUrl)
        }
        
        startService(intent)
        return id
    }

    fun pauseDownload(id: String) {
        val intent = Intent(context, DownloadService::class.java).apply {
            action = DownloadService.ACTION_PAUSE_DOWNLOAD
            putExtra(DownloadService.EXTRA_ID, id)
        }
        startService(intent)
    }

    fun resumeDownload(id: String) {
        val intent = Intent(context, DownloadService::class.java).apply {
            action = DownloadService.ACTION_RESUME_DOWNLOAD
            putExtra(DownloadService.EXTRA_ID, id)
        }
        startService(intent)
    }

    fun stopDownload(id: String) {
        val intent = Intent(context, DownloadService::class.java).apply {
            action = DownloadService.ACTION_STOP_DOWNLOAD
            putExtra(DownloadService.EXTRA_ID, id)
        }
        startService(intent)
    }

    private fun startService(intent: Intent) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    suspend fun deleteDownload(item: DownloadItem) {
        downloadDao.deleteDownload(DownloadEntity.fromDomainModel(item))
    }

    suspend fun clearAll() {
        downloadDao.deleteAll()
    }
}
