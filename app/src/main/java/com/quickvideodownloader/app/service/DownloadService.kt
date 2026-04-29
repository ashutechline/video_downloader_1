package com.quickvideodownloader.app.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.quickvideodownloader.app.R
import com.quickvideodownloader.app.data.local.dao.DownloadDao
import com.quickvideodownloader.app.data.local.entity.DownloadEntity
import com.quickvideodownloader.app.data.remote.api.VideoApi
import com.quickvideodownloader.app.data.remote.dto.DownloadRequest
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.RandomAccessFile
import javax.inject.Inject

@AndroidEntryPoint
class DownloadService : Service() {

    @Inject
    lateinit var downloadDao: DownloadDao

    @Inject
    lateinit var api: VideoApi

    @Inject
    lateinit var okHttpClient: OkHttpClient

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activeDownloads = mutableMapOf<String, DownloadTask>()

    companion object {
        private const val TAG = "DownloadService"
        const val CHANNEL_ID = "download_channel"
        
        const val ACTION_START_DOWNLOAD = "ACTION_START_DOWNLOAD"
        const val ACTION_PAUSE_DOWNLOAD = "ACTION_PAUSE_DOWNLOAD"
        const val ACTION_RESUME_DOWNLOAD = "ACTION_RESUME_DOWNLOAD"
        const val ACTION_STOP_DOWNLOAD = "ACTION_STOP_DOWNLOAD"
        
        const val EXTRA_ID = "EXTRA_ID"
        const val EXTRA_URL = "EXTRA_URL"
        const val EXTRA_FORMAT_ID = "EXTRA_FORMAT_ID"
        const val EXTRA_QUALITY = "EXTRA_QUALITY"
        const val EXTRA_TITLE = "EXTRA_TITLE"
        const val EXTRA_THUMBNAIL = "EXTRA_THUMBNAIL"
        const val EXTRA_DIRECT_URL = "EXTRA_DIRECT_URL"
    }

    data class DownloadTask(
        val id: String,
        val title: String,
        var job: Job? = null,
        var isPaused: Boolean = false
    )

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val id = intent?.getStringExtra(EXTRA_ID) ?: return START_NOT_STICKY
        
        when (intent.action) {
            ACTION_START_DOWNLOAD -> {
                val url = intent.getStringExtra(EXTRA_URL) ?: return START_NOT_STICKY
                val formatId = intent.getStringExtra(EXTRA_FORMAT_ID) ?: return START_NOT_STICKY
                val quality = intent.getStringExtra(EXTRA_QUALITY) ?: "Unknown"
                val title = intent.getStringExtra(EXTRA_TITLE) ?: "Downloading..."
                val thumbnail = intent.getStringExtra(EXTRA_THUMBNAIL)
                val directUrl = intent.getStringExtra(EXTRA_DIRECT_URL)
                
                handleStartDownload(id, url, formatId, quality, title, thumbnail, directUrl)
            }
            ACTION_PAUSE_DOWNLOAD -> handlePauseDownload(id)
            ACTION_RESUME_DOWNLOAD -> handleResumeDownload(id)
            ACTION_STOP_DOWNLOAD -> handleStopDownload(id)
        }
        
        return START_STICKY
    }

    private fun handleStartDownload(id: String, url: String, formatId: String, quality: String, title: String, thumbnail: String?, directUrl: String?) {
        if (activeDownloads.containsKey(id)) return

        val task = DownloadTask(id, title)
        activeDownloads[id] = task
        
        task.job = serviceScope.launch {
            try {
                Log.d("DownloadService", "Starting download for $id. Direct URL: ${directUrl != null}")
                
                val finalDownloadUrl: String
                val body: okhttp3.ResponseBody
                
                if (directUrl != null) {
                    // Try direct download first if we have a direct URL
                    Log.d("DownloadService", "Attempting direct download from $directUrl")
                    val request = Request.Builder().url(directUrl).build()
                    val response = okHttpClient.newCall(request).execute()
                    
                    if (response.isSuccessful && response.body != null) {
                        body = response.body!!
                        finalDownloadUrl = directUrl
                    } else {
                        Log.w("DownloadService", "Direct download failed, falling back to proxy. Code: ${response.code}")
                        // Fallback to proxy
                        val proxyResponse = api.downloadVideo(DownloadRequest(url, formatId, quality))
                        if (!proxyResponse.isSuccessful || proxyResponse.body() == null) {
                            handleError(id, title, "Download failed")
                            return@launch
                        }
                        body = proxyResponse.body()!!
                        finalDownloadUrl = "POST:api/download"
                    }
                } else {
                    // Start streaming download directly via proxy
                    val response = api.downloadVideo(DownloadRequest(url, formatId, quality))
                    Log.d("DownloadService", "Response received for $id: ${response.code()}")
                    
                    if (!response.isSuccessful) {
                        val errorMsg = response.errorBody()?.string() ?: "Server error: ${response.code()}"
                        Log.e("DownloadService", "Download failed for $id: $errorMsg")
                        handleError(id, title, errorMsg)
                        return@launch
                    }

                    val responseBody = response.body()
                    if (responseBody == null) {
                        Log.e("DownloadService", "Empty body for $id")
                        handleError(id, title, "Empty response body")
                        return@launch
                    }

                    // Check if response is actually a JSON error message instead of a video
                    val contentType = response.headers()["Content-Type"]
                    Log.d("DownloadService", "Content-Type for $id: $contentType")
                    if (contentType?.contains("application/json") == true) {
                        val errorJson = responseBody.string()
                        Log.d("DownloadService", "Error JSON for $id: $errorJson")
                        val message = try {
                            val jsonObject = com.google.gson.Gson().fromJson(errorJson, com.google.gson.JsonObject::class.java)
                            jsonObject.get("message")?.asString ?: "Server error"
                        } catch (e: Exception) {
                            "Server error"
                        }
                        handleError(id, title, message)
                        return@launch
                    }
                    body = responseBody
                    finalDownloadUrl = "POST:api/download"
                }

                // 2. Prepare file path using MediaStore
                val fileName = "video_${id.take(8)}_${System.currentTimeMillis()}.mp4"
                val uri = createMediaStoreVideoUri(fileName) ?: throw Exception("Failed to create MediaStore entry")
                val filePath = uri.toString()
                Log.d("DownloadService", "File path for $id: $filePath")

                // 3. Create initial DB entry
                val entity = DownloadEntity(
                    id = id,
                    name = title,
                    url = url,
                    downloadUrl = finalDownloadUrl,
                    formatId = formatId,
                    quality = quality,
                    progress = 0,
                    downloadedSize = 0,
                    totalSize = body.contentLength(),
                    speed = "0 KB/s",
                    isCompleted = false,
                    isPaused = false,
                    filePath = filePath,
                    thumbnail = thumbnail
                )
                downloadDao.insertDownload(entity)
                Log.d("DownloadService", "DB entry created for $id, total size: ${body.contentLength()}")

                // 4. Save the stream to file
                saveStreamToFile(id, body, uri, title)
            } catch (e: Exception) {
                Log.e("DownloadService", "Download error: ${e.message}", e)
                handleError(id, title, e.message ?: "Unknown error")
            }
        }
        
        updateForeground()
    }

    private fun handlePauseDownload(id: String) {
        val task = activeDownloads[id] ?: return
        task.isPaused = true
        task.job?.cancel()
        
        serviceScope.launch {
            downloadDao.updatePaused(id, true)
            showPausedNotification(id, task.title)
        }
    }

    private fun handleResumeDownload(id: String) {
        val existingTask = activeDownloads[id]
        if (existingTask != null && !existingTask.isPaused) return
        
        val task = existingTask ?: DownloadTask(id, "Downloading...")
        task.isPaused = false
        activeDownloads[id] = task
        
        task.job = serviceScope.launch {
            val entity = downloadDao.getDownloadById(id) ?: return@launch
            val downloadUrl = entity.downloadUrl ?: return@launch
            val uri = android.net.Uri.parse(entity.filePath)
            
            // Check current size from MediaStore
            val downloadedBytes = getFileSize(uri)
            
            // Update title from DB if we didn't have it
            if (task.title == "Downloading...") {
                activeDownloads[id] = task.copy(title = entity.name)
            }
            
            downloadDao.updatePaused(id, false)
            performDownload(id, downloadUrl, uri, entity.name, downloadedBytes)
        }
        updateForeground()
    }

    private fun handleStopDownload(id: String) {
        activeDownloads[id]?.job?.cancel()
        activeDownloads.remove(id)
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(id.hashCode())
        
        updateForeground()
    }

    private suspend fun saveStreamToFile(id: String, body: okhttp3.ResponseBody, uri: android.net.Uri, title: String) {
        try {
            val totalBytes = body.contentLength()
            val pfd = contentResolver.openFileDescriptor(uri, "rw") ?: throw Exception("Failed to open file")
            val outputStream = java.io.FileOutputStream(pfd.fileDescriptor)
            val fileChannel = outputStream.channel

            val inputStream = body.byteStream()
            val buffer = ByteArray(8192)
            var bytesRead: Int
            var downloadedBytes = 0L
            var lastUpdate = 0L
            var lastBytes = 0L
            val startTime = System.currentTimeMillis()

            // Initial notification and DB update
            updateNotification(id, title, 0, "0 KB/s", 0, totalBytes)
            downloadDao.updateProgress(id, 0, 0, totalBytes, "0 KB/s")
            Log.d("DownloadService", "Starting stream save for $id")

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                if (activeDownloads[id]?.isPaused == true) {
                    body.close()
                    return
                }
                
                fileChannel.write(java.nio.ByteBuffer.wrap(buffer, 0, bytesRead))
                downloadedBytes += bytesRead

                val currentTime = System.currentTimeMillis()
                if (currentTime - lastUpdate > 1000) { 
                    val progress = if (totalBytes > 0) (downloadedBytes * 100 / totalBytes).toInt() else 0
                    val speed = calculateSpeed(downloadedBytes - lastBytes, currentTime - lastUpdate)
                    
                    updateNotification(id, title, progress, speed, downloadedBytes, totalBytes)
                    downloadDao.updateProgress(id, progress, downloadedBytes, totalBytes, speed)
                    
                    lastUpdate = currentTime
                    lastBytes = downloadedBytes
                }
            }

            fileChannel.close()
            outputStream.close()
            pfd.close()
            body.close()
            handleSuccess(id, title, uri.toString())
        } catch (e: Exception) {
            Log.e("DownloadService", "Error saving stream: ${e.message}", e)
            if (activeDownloads[id]?.isPaused == true) {
                Log.d("DownloadService", "Download $id paused")
            } else {
                handleError(id, title, e.message ?: "Download failed")
            }
        }
    }

    private suspend fun performDownload(id: String, url: String, uri: android.net.Uri, title: String, startByte: Long) {
        // This method is now only used for Resume if the server supports Ranges
        // For now, since the main logic changed to direct POST, we might need to adapt resume too
        // but let's first fix the initial download
        val request = Request.Builder()
            .url(url)
            .apply {
                if (startByte > 0) {
                    addHeader("Range", "bytes=$startByte-")
                }
            }
            .build()
        // ... (rest of old performDownload logic or keep as is for now)
        // actually let's just use the same logic as saveStreamToFile but with a request
        try {
            val response: Response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                if (response.code == 416) { 
                    handleSuccess(id, title, uri.toString())
                } else {
                    handleError(id, title, "Server error: ${response.code}")
                }
                return
            }

            val body = response.body ?: throw Exception("Empty response body")
            saveStreamToFile(id, body, uri, title)
        } catch (e: Exception) {
            handleError(id, title, e.message ?: "Download failed")
        }
    }

    private fun createMediaStoreVideoUri(fileName: String): android.net.Uri? {
        val contentValues = android.content.ContentValues().apply {
            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_MOVIES)
            }
        }
        return contentResolver.insert(android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
    }

    private fun getFileSize(uri: android.net.Uri): Long {
        return try {
            contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize } ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    private fun calculateSpeed(bytesRead: Long, timeDiff: Long): String {
        val kbRead = bytesRead / 1024.0
        val seconds = timeDiff / 1000.0
        val speedKbps = kbRead / seconds
        return if (speedKbps > 1024) {
            String.format("%.2f MB/s", speedKbps / 1024)
        } else {
            String.format("%.1f KB/s", speedKbps)
        }
    }

    private fun handleSuccess(id: String, title: String, filePath: String) {
        serviceScope.launch {
            downloadDao.updateStatus(id, true, filePath)
            activeDownloads.remove(id)
            updateForeground()
            // Show complete notification after stopping foreground to ensure it persists
            showCompleteNotification(id, title, "Download Complete")
        }
    }

    private fun handleError(id: String, title: String, message: String) {
        serviceScope.launch {
            activeDownloads.remove(id)
            updateForeground()
            showCompleteNotification(id, title, "Failed: $message")
        }
    }

    private fun updateForeground() {
        if (activeDownloads.isEmpty()) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        } else {
            // Keep first active download as foreground notification
            val task = activeDownloads.values.first()
            val notification = createNotificationBuilder(task.id, task.title, 0, "Connecting...").build()
            startForeground(task.id.hashCode(), notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Downloads",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotificationBuilder(id: String, title: String, progress: Int, speed: String): NotificationCompat.Builder {
        val pauseIntent = Intent(this, DownloadService::class.java).apply {
            action = ACTION_PAUSE_DOWNLOAD
            putExtra(EXTRA_ID, id)
        }
        val pausePendingIntent = PendingIntent.getService(this, id.hashCode(), pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val resumeIntent = Intent(this, DownloadService::class.java).apply {
            action = ACTION_RESUME_DOWNLOAD
            putExtra(EXTRA_ID, id)
        }
        val resumePendingIntent = PendingIntent.getService(this, id.hashCode() + 1, resumeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val stopIntent = Intent(this, DownloadService::class.java).apply {
            action = ACTION_STOP_DOWNLOAD
            putExtra(EXTRA_ID, id)
        }
        val stopPendingIntent = PendingIntent.getService(this, id.hashCode() + 2, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val isPaused = activeDownloads[id]?.isPaused == true

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(if (isPaused) "Paused" else "$progress% • $speed")
            .setSmallIcon(R.drawable.ic_download)
            .setOngoing(true)
            .setProgress(100, progress, false)
            .setOnlyAlertOnce(true)
            .apply {
                if (isPaused) {
                    addAction(android.R.drawable.ic_media_play, "Resume", resumePendingIntent)
                } else {
                    addAction(android.R.drawable.ic_media_pause, "Pause", pausePendingIntent)
                }
                addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", stopPendingIntent)
            }
    }

    private fun updateNotification(id: String, title: String, progress: Int, speed: String, downloaded: Long, total: Long) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val builder = createNotificationBuilder(id, title, progress, speed)
        val downloadedMb = String.format("%.1f", downloaded / (1024.0 * 1024.0))
        val totalMb = String.format("%.1f", total / (1024.0 * 1024.0))
        builder.setContentText("$progress% • $speed • $downloadedMb/$totalMb MB")
        notificationManager.notify(id.hashCode(), builder.build())
    }

    private fun showPausedNotification(id: String, title: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val builder = createNotificationBuilder(id, title, 0, "Paused")
        notificationManager.notify(id.hashCode(), builder.build())
    }

    private fun showCompleteNotification(id: String, title: String, message: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Use a different ID offset for completed notifications so they don't get cleared by stopForeground
        val completeId = id.hashCode() + 1000
        
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_download)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setOngoing(false)
            
        notificationManager.notify(completeId, builder.build())
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
