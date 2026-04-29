package com.quickvideodownloader.app.data.local.dao

import androidx.room.*
import com.quickvideodownloader.app.data.local.entity.LockedVideoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LockedVideoDao {
    @Query("SELECT * FROM locked_videos ORDER BY lockedAt DESC")
    fun getAllLockedVideos(): Flow<List<LockedVideoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLockedVideo(video: LockedVideoEntity)

    @Delete
    suspend fun deleteLockedVideo(video: LockedVideoEntity)

    @Query("SELECT COUNT(*) FROM locked_videos")
    fun getLockedVideosCount(): Flow<Int>
    
    @Query("SELECT EXISTS(SELECT 1 FROM locked_videos WHERE id = :id)")
    suspend fun isVideoLocked(id: Long): Boolean
}
