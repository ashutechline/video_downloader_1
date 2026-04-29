package com.example.video_downloder.domain.repository

import com.example.video_downloder.domain.model.StatusItem
import kotlinx.coroutines.flow.Flow

interface StatusRepository {
    fun fetchStatuses(folderUri: String? = null): Flow<List<StatusItem>>
    suspend fun saveStatus(status: StatusItem): Result<String>
    suspend fun saveMultipleStatuses(statuses: List<StatusItem>): Result<Int>
}
