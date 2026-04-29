package com.example.video_downloder.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.video_downloder.data.local.dao.NoteDao
import com.example.video_downloder.data.local.dao.LockedVideoDao
import com.example.video_downloder.data.local.dao.HiddenVideoDao
import com.example.video_downloder.data.local.dao.RecentChatDao
import com.example.video_downloder.data.local.dao.DownloadDao
import com.example.video_downloder.data.local.entity.NoteEntity
import com.example.video_downloder.data.local.entity.LockedVideoEntity
import com.example.video_downloder.data.local.entity.HiddenVideoEntity
import com.example.video_downloder.data.local.entity.RecentChatEntity
import com.example.video_downloder.data.local.entity.DownloadEntity

@Database(
    entities = [
        NoteEntity::class, 
        LockedVideoEntity::class,
        HiddenVideoEntity::class,
        RecentChatEntity::class,
        DownloadEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract val noteDao: NoteDao
    abstract val lockedVideoDao: LockedVideoDao
    abstract val hiddenVideoDao: HiddenVideoDao
    abstract val recentChatDao: RecentChatDao
    abstract val downloadDao: DownloadDao

    companion object {
        const val DATABASE_NAME = "video_downloader_db"
    }
}
