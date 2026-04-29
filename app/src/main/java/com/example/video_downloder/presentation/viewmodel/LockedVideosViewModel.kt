package com.example.video_downloder.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.video_downloder.data.local.entity.LockedVideoEntity
import com.example.video_downloder.domain.repository.LockedVideoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class LockedVideosViewModel @Inject constructor(
    private val lockedVideoRepository: LockedVideoRepository
) : ViewModel() {

    val lockedVideos: StateFlow<List<LockedVideoEntity>> = lockedVideoRepository.getAllLockedVideos()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun unlockVideo(video: LockedVideoEntity) {
        viewModelScope.launch {
            // Logic to move back to public storage could be added here if needed
            // For now, just remove from DB
            lockedVideoRepository.unlockVideo(video)
            
            // Optionally delete the private file if we don't want to restore it
            val file = File(video.path)
            if (file.exists()) file.delete()
        }
    }
    
    fun deletePermanently(video: LockedVideoEntity) {
        viewModelScope.launch {
            lockedVideoRepository.unlockVideo(video)
            val file = File(video.path)
            if (file.exists()) file.delete()
        }
    }
}
