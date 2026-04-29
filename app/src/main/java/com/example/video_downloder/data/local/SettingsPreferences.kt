package com.example.video_downloder.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings_prefs")

@Singleton
class SettingsPreferences @Inject constructor(
    private val context: Context
) {
    companion object {
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val NOTIFICATIONS = booleanPreferencesKey("notifications")
        val AUTO_LOCK = booleanPreferencesKey("auto_lock")
        val DOWNLOAD_QUALITY = stringPreferencesKey("download_quality")
        val HAS_RATED = booleanPreferencesKey("has_rated")
        val SHOW_LATER_TIME = longPreferencesKey("show_later_time")
        val NOTIFICATION_PERMISSION_ASKED = booleanPreferencesKey("notification_permission_asked")
    }

    val darkModeFlow: Flow<Boolean> = context.settingsDataStore.data
        .map { preferences -> preferences[DARK_MODE] ?: false }

    val notificationsFlow: Flow<Boolean> = context.settingsDataStore.data
        .map { preferences -> preferences[NOTIFICATIONS] ?: true }

    val autoLockFlow: Flow<Boolean> = context.settingsDataStore.data
        .map { preferences -> preferences[AUTO_LOCK] ?: true }

    val downloadQualityFlow: Flow<String> = context.settingsDataStore.data
        .map { preferences -> preferences[DOWNLOAD_QUALITY] ?: "HD" }

    val hasRatedFlow: Flow<Boolean> = context.settingsDataStore.data
        .map { preferences -> preferences[HAS_RATED] ?: false }

    val showLaterTimeFlow: Flow<Long> = context.settingsDataStore.data
        .map { preferences -> preferences[SHOW_LATER_TIME] ?: 0L }

    val notificationPermissionAskedFlow: Flow<Boolean> = context.settingsDataStore.data
        .map { preferences -> preferences[NOTIFICATION_PERMISSION_ASKED] ?: false }

    suspend fun toggleDarkMode(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[DARK_MODE] = enabled
        }
    }

    suspend fun toggleNotifications(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[NOTIFICATIONS] = enabled
        }
    }

    suspend fun toggleAutoLock(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[AUTO_LOCK] = enabled
        }
    }

    suspend fun setDownloadQuality(quality: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[DOWNLOAD_QUALITY] = quality
        }
    }

    suspend fun setHasRated(hasRated: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[HAS_RATED] = hasRated
        }
    }

    suspend fun setShowLaterTime(time: Long) {
        context.settingsDataStore.edit { preferences ->
            preferences[SHOW_LATER_TIME] = time
        }
    }

    suspend fun setNotificationPermissionAsked(asked: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[NOTIFICATION_PERMISSION_ASKED] = asked
        }
    }

    suspend fun clearAll() {
        context.settingsDataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
