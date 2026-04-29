package com.quickvideodownloader.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.quickvideodownloader.app.data.local.dao.DownloadDao
import com.quickvideodownloader.app.data.manager.DownloadManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val downloadDao: DownloadDao,
    private val downloadManager: DownloadManager
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val downloads = downloadDao.getAllDownloads().first()
        val interruptedDownloads = downloads.filter { !it.isCompleted && !it.isPaused }
        
        for (download in interruptedDownloads) {
            // Restart downloads that were active but interrupted (e.g. app killed)
            downloadManager.resumeDownload(download.id)
        }
        
        return Result.success()
    }
}
