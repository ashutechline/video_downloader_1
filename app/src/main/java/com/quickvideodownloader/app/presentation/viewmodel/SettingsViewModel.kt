package com.quickvideodownloader.app.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quickvideodownloader.app.data.local.SettingsPreferences
import com.quickvideodownloader.app.data.local.database.AppDatabase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

data class SettingsUiState(
    val darkMode: Boolean = false,
    val notifications: Boolean = true,
    val autoLock: Boolean = true,
    val downloadQuality: String = "HD"
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsPreferences: SettingsPreferences,
    private val appDatabase: AppDatabase,
    private val application: Application
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                settingsPreferences.darkModeFlow,
                settingsPreferences.notificationsFlow,
                settingsPreferences.autoLockFlow,
                settingsPreferences.downloadQualityFlow
            ) { darkMode, notifications, autoLock, downloadQuality ->
                SettingsUiState(darkMode, notifications, autoLock, downloadQuality)
            }.collect {
                _uiState.value = it
            }
        }
    }

    fun toggleDarkMode() {
        viewModelScope.launch {
            settingsPreferences.toggleDarkMode(!_uiState.value.darkMode)
        }
    }

    fun toggleNotifications() {
        viewModelScope.launch {
            settingsPreferences.toggleNotifications(!_uiState.value.notifications)
        }
    }

    fun toggleAutoLock() {
        viewModelScope.launch {
            settingsPreferences.toggleAutoLock(!_uiState.value.autoLock)
        }
    }

    fun setDownloadQuality(quality: String) {
        viewModelScope.launch {
            settingsPreferences.setDownloadQuality(quality)
        }
    }

    fun clearCache() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                application.cacheDir.deleteRecursively()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun clearDownloads() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Clear app-specific external files (where downloads are usually stored)
                application.getExternalFilesDir(null)?.deleteRecursively()
                application.filesDir.deleteRecursively()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun resetApp() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Clear DataStore
                settingsPreferences.clearAll()
                
                // Clear Database
                appDatabase.clearAllTables()
                
                // Clear All Files
                application.cacheDir.deleteRecursively()
                application.filesDir.deleteRecursively()
                application.getExternalFilesDir(null)?.deleteRecursively()
                
                // Restarting app or notifying user is usually handled in UI
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
