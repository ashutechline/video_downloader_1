package com.quickvideodownloader.app.presentation.state

import com.quickvideodownloader.app.domain.model.StatusItem

data class StatusUiState(
    val allStatuses: List<StatusItem> = emptyList(),
    val filteredStatuses: List<StatusItem> = emptyList(),
    val selectedItems: List<StatusItem> = emptyList(),
    val selectedTab: StatusTab = StatusTab.ALL,
    val isLoading: Boolean = false,
    val isMultiSelectEnabled: Boolean = false,
    val isFolderPermissionGranted: Boolean = false,
    val folderUri: String? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

enum class StatusTab {
    ALL,
    IMAGES,
    VIDEOS
}
