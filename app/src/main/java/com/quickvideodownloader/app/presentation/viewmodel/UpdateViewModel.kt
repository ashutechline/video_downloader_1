package com.quickvideodownloader.app.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel

data class UpdateUiState(
    val showDialog: Boolean = false,
    val isForceUpdate: Boolean = false,
    val packageName: String = ""
)

@HiltViewModel
class UpdateViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(UpdateUiState(packageName = application.packageName))
    val uiState: StateFlow<UpdateUiState> = _uiState.asStateFlow()

    private val appUpdateManager = AppUpdateManagerFactory.create(application)

    init {
        checkForUpdates()
    }

    fun checkForUpdates() {
        viewModelScope.launch {
            appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
                if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                    val isImmediate = appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
                    val isFlexible = appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)

                    if (isImmediate || isFlexible) {
                        _uiState.update { 
                            it.copy(
                                showDialog = true,
                                isForceUpdate = isImmediate // If IMMEDIATE is allowed, we treat it as force update
                            )
                        }
                    }
                }
            }
        }
    }

    fun onDismissDialog() {
        _uiState.update { it.copy(showDialog = false) }
    }
}
