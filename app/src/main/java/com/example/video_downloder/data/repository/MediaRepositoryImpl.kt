package com.example.video_downloder.data.repository

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.example.video_downloder.domain.model.VideoItem
import com.example.video_downloder.domain.repository.MediaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.util.concurrent.TimeUnit

class MediaRepositoryImpl(private val context: Context) : MediaRepository {

    private val sharedPrefs = context.getSharedPreferences("media_prefs", Context.MODE_PRIVATE)

    override fun getAllVideos(): Flow<List<VideoItem>> = flow {
        val mediaList = mutableListOf<VideoItem>()
        
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media.BUCKET_ID
        )

        val query = context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            "${MediaStore.Video.Media.DATE_ADDED} DESC"
        )

        query?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val albumNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
            val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_ID)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn) ?: "Unknown"
                val path = cursor.getString(pathColumn) ?: ""
                val durationMs = cursor.getLong(durationColumn)
                val albumName = cursor.getString(albumNameColumn) ?: "Internal Storage"
                val albumId = cursor.getString(albumIdColumn) ?: "0"
                
                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    id
                ).toString()

                val isLocked = sharedPrefs.getBoolean("locked_$id", false)
                val isHidden = sharedPrefs.getBoolean("hidden_$id", false)

                mediaList.add(
                    VideoItem(
                        id = id,
                        name = name,
                        path = path,
                        albumName = albumName,
                        albumId = albumId,
                        duration = durationMs,
                        isLocked = isLocked,
                        isHidden = isHidden,
                        thumbnail = contentUri
                    )
                )
            }
        }
        emit(mediaList)
    }.flowOn(Dispatchers.IO)

    override suspend fun toggleLock(mediaId: Long) {
        val currentState = sharedPrefs.getBoolean("locked_$mediaId", false)
        sharedPrefs.edit().putBoolean("locked_$mediaId", !currentState).apply()
    }

    override suspend fun toggleHide(mediaId: Long) {
        val currentState = sharedPrefs.getBoolean("hidden_$mediaId", false)
        sharedPrefs.edit().putBoolean("hidden_$mediaId", !currentState).apply()
    }

    private fun formatDuration(durationMs: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(durationMs)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) % 60
        
        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }
}
