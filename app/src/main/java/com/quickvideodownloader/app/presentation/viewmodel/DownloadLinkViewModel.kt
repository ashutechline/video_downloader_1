package com.quickvideodownloader.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quickvideodownloader.app.data.manager.DownloadManager
import com.quickvideodownloader.app.data.remote.dto.VideoInfoResponse
import com.quickvideodownloader.app.domain.repository.VideoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.quickvideodownloader.app.data.local.SettingsPreferences
import javax.inject.Inject

data class DownloadState(
    val progress: Int = 0,
    val downloadedMB: Float = 0f,
    val totalMB: Float = 0f,
    val downloadSpeed: String = "0 KB/s",
    val videoTitle: String = "",
    val isDownloading: Boolean = false,
    val isCompleted: Boolean = false
)

sealed class DownloadLinkState {
    object Idle : DownloadLinkState()
    object Loading : DownloadLinkState()
    data class Downloading(
        val progress: Int,
        val downloadedMB: Float,
        val totalMB: Float,
        val downloadSpeed: String = "0 KB/s",
        val videoInfo: VideoInfoResponse? = null
    ) : DownloadLinkState()
    data class Success(val data: VideoInfoResponse) : DownloadLinkState()
    data class DownloadSuccess(val filePath: String, val videoInfo: VideoInfoResponse? = null) : DownloadLinkState()
    data class Error(val message: String) : DownloadLinkState()
}

@HiltViewModel
class DownloadLinkViewModel @Inject constructor(
    private val repository: VideoRepository,
    private val downloadManager: DownloadManager,
    private val settingsPreferences: SettingsPreferences
) : ViewModel() {

    private val _state = MutableStateFlow<DownloadLinkState>(DownloadLinkState.Idle)
    val state: StateFlow<DownloadLinkState> = _state.asStateFlow()

    private val _downloadState = MutableStateFlow(DownloadState())
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    val notificationPermissionAsked = settingsPreferences.notificationPermissionAskedFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private var activeDownloadId: String? = null

    init {
        // Observe downloads to update local state if our download is in progress
        downloadManager.downloads
            .onEach { downloads ->
                activeDownloadId?.let { id ->
                    val download = downloads.find { it.id == id }
                    if (download != null) {
                        if (download.isCompleted) {
                            _downloadState.value = _downloadState.value.copy(
                                isDownloading = false,
                                isCompleted = true
                            )
                            val currentInfo = (_state.value as? DownloadLinkState.Success)?.data 
                                ?: (_state.value as? DownloadLinkState.Downloading)?.videoInfo
                                ?: (_state.value as? DownloadLinkState.DownloadSuccess)?.videoInfo
                            
                            _state.value = DownloadLinkState.DownloadSuccess(download.filePath, currentInfo)
                            activeDownloadId = null // Job done
                        } else {
                            val downloadedMB = download.downloadedSize.toFloat() / (1024 * 1024)
                            val totalMB = download.totalSize.toFloat() / (1024 * 1024)
                            
                            _downloadState.value = _downloadState.value.copy(
                                progress = download.progress,
                                downloadedMB = downloadedMB,
                                totalMB = totalMB,
                                downloadSpeed = download.speed,
                                isDownloading = true
                            )
                            
                            val currentInfo = (_state.value as? DownloadLinkState.Success)?.data 
                                ?: (_state.value as? DownloadLinkState.Downloading)?.videoInfo
                            
                            _state.value = DownloadLinkState.Downloading(
                                progress = download.progress,
                                downloadedMB = downloadedMB,
                                totalMB = totalMB,
                                downloadSpeed = download.speed,
                                videoInfo = currentInfo
                            )
                        }
                    }
                }
            }.launchIn(viewModelScope)
    }

    fun fetchVideoInfo(url: String) {
        if (url.isBlank()) return
        viewModelScope.launch {
            _state.value = DownloadLinkState.Loading
            repository.getVideoInfo(url).fold(
                onSuccess = { response ->
                    _state.value = DownloadLinkState.Success(response)
                },
                onFailure = { error ->
                    _state.value = DownloadLinkState.Error(error.message ?: "An unknown error occurred")
                }
            )
        }
    }
    
    fun downloadVideo(url: String, formatId: String, quality: String) {
        val currentInfo = (_state.value as? DownloadLinkState.Success)?.data
        val selectedFormat = currentInfo?.formats?.find { it.formatId == formatId }
        val directUrl = selectedFormat?.url // Get direct URL if available
        
        _downloadState.value = DownloadState(
            isDownloading = true,
            totalMB = 0f,
            downloadedMB = 0f,
            progress = 0,
            videoTitle = currentInfo?.title ?: "Downloaded Video"
        )

        _state.value = DownloadLinkState.Downloading(
            progress = 0,
            downloadedMB = 0f,
            totalMB = 0f,
            videoInfo = currentInfo
        )

        activeDownloadId = downloadManager.startDownload(
            url = url,
            formatId = formatId,
            quality = quality,
            title = currentInfo?.title ?: "Downloaded Video",
            thumbnail = currentInfo?.thumbnail,
            directUrl = directUrl // Pass direct URL
        )
    }

    fun resetState() {
        _state.value = DownloadLinkState.Idle
        _downloadState.value = DownloadState()
        activeDownloadId = null
    }

    fun cancelDownload() {
        activeDownloadId?.let { id ->
            downloadManager.stopDownload(id)
        }
        resetState()
    }

    fun setNotificationPermissionAsked() {
        viewModelScope.launch {
            settingsPreferences.setNotificationPermissionAsked(true)
        }
    }
}
