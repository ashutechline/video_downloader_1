package com.quickvideodownloader.app.domain.repository

import com.quickvideodownloader.app.domain.model.StatusItem
import kotlinx.coroutines.flow.Flow

interface StatusRepository {
    fun fetchStatuses(folderUri: String? = null): Flow<List<StatusItem>>
    suspend fun saveStatus(status: StatusItem): Result<String>
    suspend fun saveMultipleStatuses(statuses: List<StatusItem>): Result<Int>
}
