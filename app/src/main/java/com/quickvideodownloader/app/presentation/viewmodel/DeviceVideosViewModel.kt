package com.quickvideodownloader.app.presentation.viewmodel

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import com.quickvideodownloader.app.domain.repository.LockedVideoRepository
import com.quickvideodownloader.app.data.local.entity.LockedVideoEntity
import com.quickvideodownloader.app.domain.repository.HiddenVideoRepository
import com.quickvideodownloader.app.data.local.entity.HiddenVideoEntity
import android.os.Environment
import android.os.StatFs

data class StorageUiState(
    val total: Long = 0L,
    val used: Long = 0L,
    val available: Long = 0L
)


data class LocalVideo(
    val id: Long,
    val uri: Uri,
    val name: String,
    val path: String,
    val duration: Long,
    val size: Long,
    val date: Long,
    val isLocked: Boolean = false
) {
    val durationText: String get() {
        val seconds = (duration / 1000) % 60
        val minutes = (duration / (1000 * 60)) % 60
        val hours = duration / (1000 * 60 * 60)
        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    val sizeLabel: String get() {
        val mb = size / (1024 * 1024).toDouble()
        return String.format("%.1f MB", mb)
    }
}

@HiltViewModel
class DeviceVideosViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val lockedVideoRepository: LockedVideoRepository,
    private val hiddenVideoRepository: HiddenVideoRepository
) : ViewModel() {

    private val _videos = MutableStateFlow<List<LocalVideo>>(emptyList())
    val videos: StateFlow<List<LocalVideo>> = _videos.asStateFlow()

    private val _uiEvent = MutableStateFlow<UiEvent?>(null)
    val uiEvent = _uiEvent.asStateFlow()

    private val _storageState = MutableStateFlow(StorageUiState())
    val storageState: StateFlow<StorageUiState> = _storageState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        updateStorageInfo()
    }

    private fun updateStorageInfo() {
        try {
            val stat = StatFs(Environment.getExternalStorageDirectory().path)
            val totalBytes = stat.totalBytes
            val availableBytes = stat.availableBytes
            val usedBytes = totalBytes - availableBytes
            
            _storageState.value = StorageUiState(
                total = totalBytes,
                used = usedBytes,
                available = availableBytes
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun formatSize(bytes: Long): String {
        val gb = bytes / (1024 * 1024 * 1024).toDouble()
        return String.format("%.1f GB", gb)
    }

    fun loadVideos() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            val videoList = mutableListOf<LocalVideo>()
            val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            val projection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DATA,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.DATE_ADDED
            )

            val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

            context.contentResolver.query(
                collection,
                projection,
                null,
                null,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn) ?: "Unknown"
                    val path = cursor.getString(pathColumn) ?: ""
                    val duration = cursor.getLong(durationColumn)
                    val size = cursor.getLong(sizeColumn)
                    val date = cursor.getLong(dateColumn)

                    val contentUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)

                    videoList.add(
                        LocalVideo(
                            id = id,
                            uri = contentUri,
                            name = name,
                            path = path,
                            duration = duration,
                            size = size,
                            date = date
                        )
                    )
                }
            }
            _videos.value = videoList
            _isLoading.value = false
        }
    }

    fun deleteVideo(video: LocalVideo) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val deletedRows = context.contentResolver.delete(video.uri, null, null)
                if (deletedRows > 0) {
                    _videos.update { list -> list.filter { it.id != video.id } }
                    _uiEvent.value = UiEvent.ShowSnackbar("Video deleted successfully")
                }
            } catch (e: Exception) {
                _uiEvent.value = UiEvent.ShowSnackbar("Failed to delete video: ${e.message}")
            }
        }
    }

    fun lockVideo(video: LocalVideo) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val sourceFile = File(video.path)
                if (!sourceFile.exists()) {
                    _uiEvent.value = UiEvent.ShowSnackbar("Source file not found")
                    return@launch
                }

                val lockedDir = File(context.filesDir, "locked_videos")
                if (!lockedDir.exists()) lockedDir.mkdirs()

                val destFile = File(lockedDir, video.name)
                sourceFile.copyTo(destFile, overwrite = true)
                
                // Save to database
                val entity = LockedVideoEntity(
                    id = video.id,
                    name = video.name,
                    path = destFile.absolutePath,
                    duration = video.duration,
                    size = video.size,
                    lockedAt = System.currentTimeMillis()
                )
                lockedVideoRepository.lockVideo(entity)

                // After copying to private folder and saving to DB, delete from public MediaStore
                context.contentResolver.delete(video.uri, null, null)
                
                _videos.update { list -> list.filter { it.id != video.id } }
                _uiEvent.value = UiEvent.ShowSnackbar("Video moved to vault")
            } catch (e: Exception) {
                _uiEvent.value = UiEvent.ShowSnackbar("Failed to lock video: ${e.message}")
            }
        }
    }

    fun hideVideo(video: LocalVideo) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val sourceFile = File(video.path)
                if (!sourceFile.exists()) {
                    _uiEvent.value = UiEvent.ShowSnackbar("Source file not found")
                    return@launch
                }

                val hiddenDir = File(context.filesDir, ".hidden_videos")
                if (!hiddenDir.exists()) hiddenDir.mkdirs()

                val destFile = File(hiddenDir, video.name)
                sourceFile.copyTo(destFile, overwrite = true)
                
                // Save to database
                val entity = HiddenVideoEntity(
                    id = video.id,
                    name = video.name,
                    path = destFile.absolutePath,
                    originalPath = video.path,
                    duration = video.duration,
                    size = video.size,
                    hiddenAt = System.currentTimeMillis()
                )
                hiddenVideoRepository.hideVideo(entity)

                // After copying to private folder and saving to DB, delete from public MediaStore
                context.contentResolver.delete(video.uri, null, null)
                
                _videos.update { list -> list.filter { it.id != video.id } }
                _uiEvent.value = UiEvent.ShowSnackbar("Video hidden successfully")
            } catch (e: Exception) {
                _uiEvent.value = UiEvent.ShowSnackbar("Failed to hide video: ${e.message}")
            }
        }
    }

    fun clearUiEvent() {
        _uiEvent.value = null
    }

    sealed class UiEvent {
        data class ShowSnackbar(val message: String) : UiEvent()
    }
}
