package com.quickvideodownloader.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quickvideodownloader.app.domain.model.Album
import com.quickvideodownloader.app.domain.model.VideoItem
import com.quickvideodownloader.app.domain.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class CollectionTab {
    All, Videos, Locked, Hidden, Albums
}

@HiltViewModel
class CollectionViewModel @Inject constructor(
    private val repository: MediaRepository
) : ViewModel() {

    private val _allMedia = MutableStateFlow<List<VideoItem>>(emptyList())
    val allMedia: StateFlow<List<VideoItem>> = _allMedia.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _selectedTab = MutableStateFlow(CollectionTab.Albums)
    val selectedTab: StateFlow<CollectionTab> = _selectedTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _albumList = MutableStateFlow<List<Album>>(emptyList())
    val albumList: StateFlow<List<Album>> = _albumList.asStateFlow()

    val filteredMedia: StateFlow<List<VideoItem>> = combine(_allMedia, _selectedTab, _searchQuery) { media, tab, query ->
        val tabFiltered = when (tab) {
            CollectionTab.All -> media
            CollectionTab.Videos -> media
            CollectionTab.Locked -> media.filter { it.isLocked }
            CollectionTab.Hidden -> media.filter { it.isHidden }
            CollectionTab.Albums -> emptyList() // Albums handled separately in UI
        }
        
        if (query.isBlank()) {
            tabFiltered
        } else {
            tabFiltered.filter { it.name.contains(query, ignoreCase = true) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadMedia()
    }

    fun loadMedia() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getAllVideos().collect { videos ->
                _allMedia.value = videos
                groupVideosByAlbum(videos)
                _isLoading.value = false
            }
        }
    }

    private fun groupVideosByAlbum(videos: List<VideoItem>) {
        val grouped = videos.groupBy { it.albumId }
        val albums = grouped.map { (albumId, videoList) ->
            Album(
                id = albumId,
                name = videoList.firstOrNull()?.albumName ?: "Unknown",
                videos = videoList,
                thumbnail = videoList.firstOrNull()?.thumbnail ?: ""
            )
        }
        _albumList.value = albums
    }

    fun selectTab(tab: CollectionTab) {
        _selectedTab.value = tab
    }

    fun updateQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleLock(mediaId: Long) {
        viewModelScope.launch {
            repository.toggleLock(mediaId)
            loadMedia() // Reload to reflect changes
        }
    }

    fun toggleHide(mediaId: Long) {
        viewModelScope.launch {
            repository.toggleHide(mediaId)
            loadMedia() // Reload to reflect changes
        }
    }
}
