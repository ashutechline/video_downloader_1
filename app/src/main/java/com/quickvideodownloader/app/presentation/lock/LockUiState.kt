package com.quickvideodownloader.app.presentation.lock

data class LockUiState(
    val enteredPin: String = "",
    val savedPinHash: String? = null,
    val isError: Boolean = false,
    val isFirstTime: Boolean = false,
    val isUnlocked: Boolean = false,
    val isAutoLockEnabled: Boolean = true
)
