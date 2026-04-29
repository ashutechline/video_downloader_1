package com.example.video_downloder.data.repository

import com.example.video_downloder.data.local.dao.LockedVideoDao
import com.example.video_downloder.data.local.entity.LockedVideoEntity
import com.example.video_downloder.domain.repository.LockedVideoRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class LockedVideoRepositoryImpl @Inject constructor(
    private val lockedVideoDao: LockedVideoDao
) : LockedVideoRepository {

    override fun getAllLockedVideos(): Flow<List<LockedVideoEntity>> {
        return lockedVideoDao.getAllLockedVideos()
    }

    override suspend fun lockVideo(video: LockedVideoEntity) {
        lockedVideoDao.insertLockedVideo(video)
    }

    override suspend fun unlockVideo(video: LockedVideoEntity) {
        lockedVideoDao.deleteLockedVideo(video)
    }

    override fun getLockedVideosCount(): Flow<Int> {
        return lockedVideoDao.getLockedVideosCount()
    }

    override suspend fun isVideoLocked(id: Long): Boolean {
        return lockedVideoDao.isVideoLocked(id)
    }
}
