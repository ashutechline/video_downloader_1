package com.example.video_downloder.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.video_downloder.data.local.SettingsPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RatingUiState(
    val showDialog: Boolean = false,
    val selectedRating: Int = 0,
    val hasRated: Boolean = false,
    val showLaterTime: Long = 0L
)

@HiltViewModel
class RatingViewModel @Inject constructor(
    private val settingsPreferences: SettingsPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(RatingUiState())
    val uiState: StateFlow<RatingUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                settingsPreferences.hasRatedFlow,
                settingsPreferences.showLaterTimeFlow
            ) { hasRated, showLaterTime ->
                Pair(hasRated, showLaterTime)
            }.collect { (hasRated, showLaterTime) ->
                _uiState.update { it.copy(hasRated = hasRated, showLaterTime = showLaterTime) }
            }
        }
    }

    fun onShowDialog() {
        _uiState.update { it.copy(showDialog = true) }
    }

    fun onDismissDialog() {
        _uiState.update { it.copy(showDialog = false, selectedRating = 0) }
    }

    fun onRatingSelected(rating: Int) {
        _uiState.update { it.copy(selectedRating = rating) }
    }

    fun submitRating(rating: Int, onRedirect: () -> Unit) {
        viewModelScope.launch {
            if (rating >= 4) {
                settingsPreferences.setHasRated(true)
                onRedirect()
            }
            onDismissDialog()
        }
    }

    fun onShowLater() {
        viewModelScope.launch {
            val nextDay = System.currentTimeMillis() + (24 * 60 * 60 * 1000) // 24 hours
            settingsPreferences.setShowLaterTime(nextDay)
            onDismissDialog()
        }
    }
}
