package com.quickvideodownloader.app.data.local.dao

import androidx.room.*
import com.quickvideodownloader.app.data.local.entity.HiddenVideoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HiddenVideoDao {
    @Query("SELECT * FROM hidden_videos ORDER BY hiddenAt DESC")
    fun getAllHiddenVideos(): Flow<List<HiddenVideoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHiddenVideo(video: HiddenVideoEntity)

    @Delete
    suspend fun deleteHiddenVideo(video: HiddenVideoEntity)

    @Query("SELECT COUNT(*) FROM hidden_videos")
    fun getHiddenVideosCount(): Flow<Int>

    @Query("SELECT * FROM hidden_videos")
    suspend fun getAllHiddenVideosList(): List<HiddenVideoEntity>
}
