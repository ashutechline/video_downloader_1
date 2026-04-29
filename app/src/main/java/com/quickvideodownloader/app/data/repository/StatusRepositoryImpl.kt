package com.quickvideodownloader.app.data.repository

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.documentfile.provider.DocumentFile
import com.quickvideodownloader.app.domain.model.FileType
import com.quickvideodownloader.app.domain.model.StatusItem
import com.quickvideodownloader.app.domain.repository.StatusRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class StatusRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : StatusRepository {

    private val whatsappPaths = listOf(
        "${Environment.getExternalStorageDirectory()}/Android/media/com.whatsapp/WhatsApp/Media/.Statuses",
        "${Environment.getExternalStorageDirectory()}/WhatsApp/Media/.Statuses",
        "${Environment.getExternalStorageDirectory()}/Android/media/com.whatsapp.w4b/WhatsApp Business/Media/.Statuses",
        "${Environment.getExternalStorageDirectory()}/WhatsApp Business/Media/.Statuses"
    )
    private val saveDirectory = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "MyApp")

    override fun fetchStatuses(folderUri: String?): Flow<List<StatusItem>> = flow {
        val statuses = mutableListOf<StatusItem>()
        
        if (folderUri != null) {
            // Android 11+ SAF approach
            val treeUri = Uri.parse(folderUri)
            val root = DocumentFile.fromTreeUri(context, treeUri)
            root?.listFiles()?.filter { file ->
                file.isFile && (file.name?.endsWith(".jpg", true) == true || 
                                file.name?.endsWith(".png", true) == true || 
                                file.name?.endsWith(".mp4", true) == true)
            }?.sortedByDescending { it.lastModified() }?.forEach { file ->
                val name = file.name ?: "Status"
                val fileType = if (name.endsWith(".mp4", true)) FileType.VIDEO else FileType.IMAGE
                
                statuses.add(
                    StatusItem(
                        id = file.uri.toString(),
                        name = name,
                        uri = file.uri,
                        fileType = fileType,
                        extension = name.substringAfterLast('.', ""),
                        size = file.length(),
                        dateModified = file.lastModified(),
                        duration = if (fileType == FileType.VIDEO) getDuration(file.uri) else null,
                        isSaved = checkIfSaved(name)
                    )
                )
            }
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            // Android 10 and below direct file approach - check all potential paths
            whatsappPaths.forEach { pathStr ->
                val directory = File(pathStr)
                if (directory.exists() && directory.isDirectory) {
                    directory.listFiles()?.filter { file ->
                        file.isFile && (file.extension.equals("jpg", true) || 
                                        file.extension.equals("png", true) || 
                                        file.extension.equals("mp4", true))
                    }?.forEach { file ->
                        val fileType = if (file.extension.equals("mp4", true)) FileType.VIDEO else FileType.IMAGE
                        statuses.add(
                            StatusItem(
                                id = file.absolutePath,
                                name = file.name,
                                uri = Uri.fromFile(file),
                                fileType = fileType,
                                extension = file.extension,
                                size = file.length(),
                                dateModified = file.lastModified(),
                                duration = if (fileType == FileType.VIDEO) getDuration(Uri.fromFile(file)) else null,
                                isSaved = checkIfSaved(file.name)
                            )
                        )
                    }
                }
            }
        }
        // Emit sorted list (newest first) as seen in the Flutter reference
        emit(statuses.sortedByDescending { it.dateModified })
    }.flowOn(Dispatchers.IO)

    override suspend fun saveStatus(status: StatusItem): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (!saveDirectory.exists()) {
                saveDirectory.mkdirs()
            }

            val destFile = File(saveDirectory, status.name)
            if (destFile.exists()) {
                return@withContext Result.success("Already saved")
            }

            context.contentResolver.openInputStream(status.uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }

            // Trigger media scan
            android.media.MediaScannerConnection.scanFile(
                context,
                arrayOf(destFile.absolutePath),
                arrayOf(if (status.fileType == FileType.VIDEO) "video/mp4" else "image/jpeg"),
                null
            )

            Result.success("Saved successfully")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveMultipleStatuses(statuses: List<StatusItem>): Result<Int> = withContext(Dispatchers.IO) {
        var count = 0
        try {
            statuses.forEach { status ->
                val result = saveStatus(status)
                if (result.isSuccess) count++
            }
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun getDuration(uri: Uri): String {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)
            val time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val timeInMillis = time?.toLong() ?: 0L
            retriever.release()
            
            val minutes = TimeUnit.MILLISECONDS.toMinutes(timeInMillis)
            val seconds = TimeUnit.MILLISECONDS.toSeconds(timeInMillis) - TimeUnit.MINUTES.toSeconds(minutes)
            String.format("%d:%02d", minutes, seconds)
        } catch (e: Exception) {
            "00:00"
        }
    }

    private fun checkIfSaved(fileName: String): Boolean {
        return File(saveDirectory, fileName).exists()
    }
}
