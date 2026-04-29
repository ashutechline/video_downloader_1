package com.quickvideodownloader.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quickvideodownloader.app.domain.repository.LockedVideoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

import com.quickvideodownloader.app.domain.repository.HiddenVideoRepository
import com.quickvideodownloader.app.data.local.SettingsPreferences
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val lockedVideoRepository: LockedVideoRepository,
    private val hiddenVideoRepository: HiddenVideoRepository,
    private val settingsPreferences: SettingsPreferences
) : ViewModel() {

    val notificationPermissionAsked = settingsPreferences.notificationPermissionAskedFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val lockedVideosCount: StateFlow<Int> = lockedVideoRepository.getLockedVideosCount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    val hiddenVideosCount: StateFlow<Int> = hiddenVideoRepository.getHiddenVideosCount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    fun setNotificationPermissionAsked() {
        viewModelScope.launch {
            settingsPreferences.setNotificationPermissionAsked(true)
        }
    }
}

