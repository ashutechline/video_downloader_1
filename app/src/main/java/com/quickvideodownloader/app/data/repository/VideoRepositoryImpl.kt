package com.quickvideodownloader.app.data.repository

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.quickvideodownloader.app.data.remote.api.VideoApi
import com.quickvideodownloader.app.data.remote.dto.DownloadRequest
import com.quickvideodownloader.app.data.remote.dto.VideoInfoRequest
import com.quickvideodownloader.app.data.remote.dto.VideoInfoResponse
import com.quickvideodownloader.app.domain.repository.DownloadStatus
import com.quickvideodownloader.app.domain.repository.VideoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.ResponseBody
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

class VideoRepositoryImpl(
    private val api: VideoApi,
    private val context: Context
) : VideoRepository {
    override suspend fun getVideoInfo(url: String): Result<VideoInfoResponse> {
        return try {
            val response = api.getVideoInfo(VideoInfoRequest(url = url))
            if (response.success) {
                Result.success(response)
            } else {
                Result.failure(Exception("Failed to fetch video info"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun downloadVideo(
        url: String,
        formatId: String,
        quality: String
    ): Flow<DownloadStatus> = flow {
        try {
            val response = api.downloadVideo(
                DownloadRequest(
                    url = url,
                    formatId = formatId,
                    format = quality
                )
            )

            if (!response.isSuccessful) {
                emit(DownloadStatus.Error("Server error: ${response.code()}"))
                return@flow
            }

            val body = response.body() ?: run {
                emit(DownloadStatus.Error("Empty response body"))
                return@flow
            }

            // Check if we got JSON instead of a video stream (error case from some servers)
            val contentType = response.headers()["Content-Type"]
            if (contentType?.contains("application/json", ignoreCase = true) == true) {
                val jsonString = body.string()
                try {
                    val jsonObject = org.json.JSONObject(jsonString)
                    val message = jsonObject.optString("message", "Download failed")
                    emit(DownloadStatus.Error(message))
                } catch (e: Exception) {
                    emit(DownloadStatus.Error("Download failed: Invalid response format"))
                }
                return@flow
            }

            val fileName = "video_${System.currentTimeMillis()}.mp4"
            
            saveFileWithProgress(body, fileName) { progress, downloaded, total ->
                emit(DownloadStatus.Progress(progress, downloaded, total))
            }?.let { path ->
                emit(DownloadStatus.Success(path))
            } ?: run {
                emit(DownloadStatus.Error("Failed to save video file"))
            }
        } catch (e: Exception) {
            emit(DownloadStatus.Error(e.message ?: "An unknown error occurred"))
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun saveFileWithProgress(
        body: ResponseBody, 
        fileName: String, 
        onProgress: suspend (Int, Long, Long) -> Unit
    ): String? {
        val totalBytes = body.contentLength()
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
        }

        val resolver = context.contentResolver
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Downloads.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        val uri = resolver.insert(collection, contentValues)

        return uri?.let {
            try {
                resolver.openOutputStream(it)?.use { outputStream ->
                    body.byteStream().use { inputStream ->
                        copyStreamWithProgress(inputStream, outputStream, totalBytes, onProgress)
                    }
                }
                it.toString()
            } catch (e: Exception) {
                null
            }
        } ?: run {
            val file = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                fileName
            )
            try {
                FileOutputStream(file).use { outputStream ->
                    body.byteStream().use { inputStream ->
                        copyStreamWithProgress(inputStream, outputStream, totalBytes, onProgress)
                    }
                }
                file.absolutePath
            } catch (e: Exception) {
                null
            }
        }
    }

    private suspend fun copyStreamWithProgress(
        inputStream: InputStream,
        outputStream: OutputStream,
        totalBytes: Long,
        onProgress: suspend (Int, Long, Long) -> Unit
    ) {
        val buffer = ByteArray(1024)
        var bytesRead: Int
        var totalBytesRead = 0L

        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            outputStream.write(buffer, 0, bytesRead)
            totalBytesRead += bytesRead

            val progress = if (totalBytes > 0) {
                (totalBytesRead * 100 / totalBytes).toInt()
            } else {
                0
            }
            onProgress(progress, totalBytesRead, totalBytes)
        }
    }
}
