package com.quickvideodownloader.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quickvideodownloader.app.data.manager.DownloadManager
import com.quickvideodownloader.app.domain.model.DownloadItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class DownloadTab {
    ALL, DOWNLOADING, COMPLETED
}

@HiltViewModel
class DownloadViewModel @Inject constructor(
    private val downloadManager: DownloadManager
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(DownloadTab.ALL)
    val selectedTab: StateFlow<DownloadTab> = _selectedTab.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val downloads = combine(downloadManager.downloads, _selectedTab) { downloads, tab ->
        _isLoading.value = false
        when (tab) {
            DownloadTab.ALL -> downloads
            DownloadTab.DOWNLOADING -> downloads.filter { !it.isCompleted }
            DownloadTab.COMPLETED -> downloads.filter { it.isCompleted }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectTab(tab: DownloadTab) {
        _selectedTab.value = tab
    }

    fun togglePause(id: String) {
        val download = downloads.value.find { it.id == id } ?: return
        if (download.isPaused) {
            downloadManager.resumeDownload(id)
        } else {
            downloadManager.pauseDownload(id)
        }
    }

    fun cancelDownload(id: String) {
        downloadManager.stopDownload(id)
    }

    fun deleteDownload(id: String) {
        viewModelScope.launch {
            downloads.value.find { it.id == id }?.let {
                downloadManager.deleteDownload(it)
            }
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            downloadManager.clearAll()
        }
    }
}
