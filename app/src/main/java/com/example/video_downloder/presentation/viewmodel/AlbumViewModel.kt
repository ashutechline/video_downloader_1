package com.example.video_downloder.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.video_downloder.domain.model.Album
import com.example.video_downloder.domain.model.VideoItem
import com.example.video_downloder.domain.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlbumViewModel @Inject constructor(
    private val repository: MediaRepository
) : ViewModel() {

    private val _allVideos = MutableStateFlow<List<VideoItem>>(emptyList())
    
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _selectedAlbumId = MutableStateFlow<String?>(null)

    val albumList: StateFlow<List<Album>> = _allVideos.map { videos ->
        groupVideosIntoAlbums(videos)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedAlbumVideos: StateFlow<List<VideoItem>> = combine(_allVideos, _selectedAlbumId) { videos, albumId ->
        if (albumId == null) emptyList()
        else videos.filter { it.albumId == albumId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadVideos()
    }

    private fun loadVideos() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getAllVideos().collect { videos ->
                _allVideos.value = videos
                _isLoading.value = false
            }
        }
    }

    private fun groupVideosIntoAlbums(videos: List<VideoItem>): List<Album> {
        val grouped = videos.groupBy { it.albumId }
        return grouped.map { (albumId, videoList) ->
            Album(
                id = albumId,
                name = videoList.firstOrNull()?.albumName ?: "Unknown",
                videos = videoList,
                thumbnail = videoList.firstOrNull()?.thumbnail ?: ""
            )
        }
    }

    fun loadVideosByAlbum(albumId: String) {
        _selectedAlbumId.value = albumId
    }
}
