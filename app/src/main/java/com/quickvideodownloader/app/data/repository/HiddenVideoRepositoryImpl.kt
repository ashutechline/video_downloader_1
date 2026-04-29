package com.quickvideodownloader.app.data.repository

import com.quickvideodownloader.app.data.local.dao.HiddenVideoDao
import com.quickvideodownloader.app.data.local.entity.HiddenVideoEntity
import com.quickvideodownloader.app.domain.repository.HiddenVideoRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class HiddenVideoRepositoryImpl @Inject constructor(
    private val hiddenVideoDao: HiddenVideoDao
) : HiddenVideoRepository {

    override fun getAllHiddenVideos(): Flow<List<HiddenVideoEntity>> {
        return hiddenVideoDao.getAllHiddenVideos()
    }

    override suspend fun hideVideo(video: HiddenVideoEntity) {
        hiddenVideoDao.insertHiddenVideo(video)
    }

    override suspend fun unhideVideo(video: HiddenVideoEntity) {
        hiddenVideoDao.deleteHiddenVideo(video)
    }

    override suspend fun deleteVideo(video: HiddenVideoEntity) {
        hiddenVideoDao.deleteHiddenVideo(video)
    }

    override fun getHiddenVideosCount(): Flow<Int> {
        return hiddenVideoDao.getHiddenVideosCount()
    }

    override suspend fun getAllHiddenVideosList(): List<HiddenVideoEntity> {
        return hiddenVideoDao.getAllHiddenVideosList()
    }
}
