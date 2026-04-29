package com.example.video_downloder.data.local.dao

import androidx.room.*
import com.example.video_downloder.data.local.entity.DownloadEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY timestamp DESC")
    fun getAllDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE id = :id")
    suspend fun getDownloadById(id: String): DownloadEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(download: DownloadEntity)

    @Update
    suspend fun updateDownload(download: DownloadEntity)

    @Delete
    suspend fun deleteDownload(download: DownloadEntity)

    @Query("DELETE FROM downloads")
    suspend fun deleteAll()
    
    @Query("UPDATE downloads SET progress = :progress, downloadedSize = :downloadedSize, totalSize = :totalSize, speed = :speed WHERE id = :id")
    suspend fun updateProgress(id: String, progress: Int, downloadedSize: Long, totalSize: Long, speed: String)

    @Query("UPDATE downloads SET isPaused = :isPaused WHERE id = :id")
    suspend fun updatePaused(id: String, isPaused: Boolean)

    @Query("UPDATE downloads SET isCompleted = :isCompleted, filePath = :filePath WHERE id = :id")
    suspend fun updateStatus(id: String, isCompleted: Boolean, filePath: String)
}
