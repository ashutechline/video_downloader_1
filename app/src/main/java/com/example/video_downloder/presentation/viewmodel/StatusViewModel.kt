package com.example.video_downloder.presentation.viewmodel

import android.content.Context
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.video_downloder.domain.model.FileType
import com.example.video_downloder.domain.model.StatusItem
import com.example.video_downloder.domain.repository.StatusRepository
import com.example.video_downloder.presentation.state.StatusTab
import com.example.video_downloder.presentation.state.StatusUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StatusViewModel @Inject constructor(
    private val repository: StatusRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val sharedPrefs = context.getSharedPreferences("status_saver_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(StatusUiState())
    val uiState: StateFlow<StatusUiState> = _uiState.asStateFlow()

    init {
        val savedUri = sharedPrefs.getString("folder_uri", null)
        val isGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            savedUri != null
        } else {
            true // Below Android 11 we use standard permissions
        }
        
        _uiState.update { it.copy(
            folderUri = savedUri,
            isFolderPermissionGranted = isGranted
        ) }
        
        if (isGranted) {
            loadStatuses()
        }
    }

    fun onFolderPermissionGranted(uri: String) {
        sharedPrefs.edit().putString("folder_uri", uri).apply()
        _uiState.update { it.copy(
            folderUri = uri,
            isFolderPermissionGranted = true
        ) }
        loadStatuses()
    }

    fun loadStatuses() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.fetchStatuses(_uiState.value.folderUri).collect { statuses ->
                _uiState.update { state ->
                    state.copy(
                        allStatuses = statuses,
                        filteredStatuses = filterStatuses(statuses, state.selectedTab),
                        isLoading = false
                    )
                }
            }
        }
    }

    fun selectTab(tab: StatusTab) {
        _uiState.update { state ->
            state.copy(
                selectedTab = tab,
                filteredStatuses = filterStatuses(state.allStatuses, tab)
            )
        }
    }

    fun toggleSelection(status: StatusItem) {
        _uiState.update { state ->
            val updatedAll = state.allStatuses.map {
                if (it.id == status.id) it.copy(isSelected = !it.isSelected) else it
            }
            val updatedFiltered = state.filteredStatuses.map {
                if (it.id == status.id) it.copy(isSelected = !it.isSelected) else it
            }
            val selectedItems = updatedAll.filter { it.isSelected }
            
            state.copy(
                allStatuses = updatedAll,
                filteredStatuses = updatedFiltered,
                selectedItems = selectedItems,
                isMultiSelectEnabled = selectedItems.isNotEmpty()
            )
        }
    }

    fun clearSelection() {
        _uiState.update { state ->
            val updatedAll = state.allStatuses.map { it.copy(isSelected = false) }
            val updatedFiltered = state.filteredStatuses.map { it.copy(isSelected = false) }
            
            state.copy(
                allStatuses = updatedAll,
                filteredStatuses = updatedFiltered,
                selectedItems = emptyList(),
                isMultiSelectEnabled = false
            )
        }
    }

    fun saveStatus(status: StatusItem) {
        viewModelScope.launch {
            val result = repository.saveStatus(status)
            result.onSuccess { message ->
                _uiState.update { it.copy(successMessage = message) }
                markAsSaved(status.id)
            }.onFailure { error ->
                _uiState.update { it.copy(errorMessage = error.message) }
            }
        }
    }

    fun saveSelected() {
        viewModelScope.launch {
            val selected = _uiState.value.selectedItems
            if (selected.isEmpty()) return@launch

            val result = repository.saveMultipleStatuses(selected)
            result.onSuccess { count ->
                _uiState.update { it.copy(successMessage = "Successfully saved $count items") }
                selected.forEach { markAsSaved(it.id) }
                clearSelection()
            }.onFailure { error ->
                _uiState.update { it.copy(errorMessage = error.message) }
            }
        }
    }

    private fun markAsSaved(id: String) {
        _uiState.update { state ->
            val updatedAll = state.allStatuses.map {
                if (it.id == id) it.copy(isSaved = true) else it
            }
            val updatedFiltered = state.filteredStatuses.map {
                if (it.id == id) it.copy(isSaved = true) else it
            }
            state.copy(allStatuses = updatedAll, filteredStatuses = updatedFiltered)
        }
    }

    private fun filterStatuses(statuses: List<StatusItem>, tab: StatusTab): List<StatusItem> {
        return when (tab) {
            StatusTab.ALL -> statuses
            StatusTab.IMAGES -> statuses.filter { it.fileType == FileType.IMAGE }
            StatusTab.VIDEOS -> statuses.filter { it.fileType == FileType.VIDEO }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(successMessage = null, errorMessage = null) }
    }
}
