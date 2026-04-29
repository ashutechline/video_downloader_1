package com.example.video_downloder.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.video_downloder.data.local.entity.HiddenVideoEntity
import com.example.video_downloder.domain.repository.HiddenVideoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class HiddenVideosViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val hiddenVideoRepository: HiddenVideoRepository
) : ViewModel() {

    val hiddenVideos: StateFlow<List<HiddenVideoEntity>> = hiddenVideoRepository.getAllHiddenVideos()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _uiEvent = MutableStateFlow<UiEvent?>(null)
    val uiEvent = _uiEvent.asStateFlow()

    fun unhideVideo(video: HiddenVideoEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val sourceFile = File(video.path)
                val destFile = File(video.originalPath)
                
                if (sourceFile.exists()) {
                    sourceFile.copyTo(destFile, overwrite = true)
                    sourceFile.delete()
                    
                    hiddenVideoRepository.unhideVideo(video)
                    _uiEvent.value = UiEvent.ShowSnackbar("Video unhidden")
                } else {
                    _uiEvent.value = UiEvent.ShowSnackbar("Source file not found")
                }
            } catch (e: Exception) {
                _uiEvent.value = UiEvent.ShowSnackbar("Failed to unhide: ${e.message}")
            }
        }
    }

    fun unhideAll() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val list = hiddenVideoRepository.getAllHiddenVideosList()
                var count = 0
                list.forEach { video ->
                    val sourceFile = File(video.path)
                    val destFile = File(video.originalPath)
                    if (sourceFile.exists()) {
                        sourceFile.copyTo(destFile, overwrite = true)
                        sourceFile.delete()
                        hiddenVideoRepository.unhideVideo(video)
                        count++
                    }
                }
                _uiEvent.value = UiEvent.ShowSnackbar("$count videos unhidden")
            } catch (e: Exception) {
                _uiEvent.value = UiEvent.ShowSnackbar("Failed to unhide all: ${e.message}")
            }
        }
    }

    fun deletePermanently(video: HiddenVideoEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = File(video.path)
                if (file.exists()) file.delete()
                hiddenVideoRepository.deleteVideo(video)
                _uiEvent.value = UiEvent.ShowSnackbar("Video deleted permanently")
            } catch (e: Exception) {
                _uiEvent.value = UiEvent.ShowSnackbar("Failed to delete: ${e.message}")
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
