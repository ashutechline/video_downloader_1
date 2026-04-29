package com.quickvideodownloader.app.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.quickvideodownloader.app.data.local.dao.NoteDao
import com.quickvideodownloader.app.data.local.dao.LockedVideoDao
import com.quickvideodownloader.app.data.local.dao.HiddenVideoDao
import com.quickvideodownloader.app.data.local.dao.RecentChatDao
import com.quickvideodownloader.app.data.local.dao.DownloadDao
import com.quickvideodownloader.app.data.local.entity.NoteEntity
import com.quickvideodownloader.app.data.local.entity.LockedVideoEntity
import com.quickvideodownloader.app.data.local.entity.HiddenVideoEntity
import com.quickvideodownloader.app.data.local.entity.RecentChatEntity
import com.quickvideodownloader.app.data.local.entity.DownloadEntity

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
