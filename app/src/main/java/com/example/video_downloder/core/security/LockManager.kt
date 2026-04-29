package com.example.video_downloder.core.security

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "security_prefs")

@Singleton
class LockManager @Inject constructor(@ApplicationContext private val context: Context) {


    companion object {
        private val PIN_KEY = stringPreferencesKey("user_pin_hash")
        private val AUTO_LOCK_KEY = booleanPreferencesKey("auto_lock")
    }

    val savedPinHash: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[PIN_KEY]
    }

    val isAutoLockEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[AUTO_LOCK_KEY] ?: true
    }

    suspend fun savePin(pin: String) {
        val hash = hashPin(pin)
        context.dataStore.edit { preferences ->
            preferences[PIN_KEY] = hash
        }
    }

    suspend fun setAutoLock(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_LOCK_KEY] = enabled
        }
    }

    fun hashPin(pin: String): String {
        val bytes = pin.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }

    suspend fun clearPin() {
        context.dataStore.edit { preferences ->
            preferences.remove(PIN_KEY)
        }
    }
}
