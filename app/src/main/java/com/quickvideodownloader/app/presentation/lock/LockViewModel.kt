package com.quickvideodownloader.app.presentation.lock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quickvideodownloader.app.core.security.LockManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LockViewModel @Inject constructor(
    private val lockManager: LockManager
) : ViewModel() {

    private val _state = MutableStateFlow(LockUiState())
    val state: StateFlow<LockUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            lockManager.savedPinHash.collect { hash ->
                _state.update { 
                    it.copy(
                        savedPinHash = hash,
                        isFirstTime = hash == null
                    )
                }
            }
        }
        viewModelScope.launch {
            lockManager.isAutoLockEnabled.collect { enabled ->
                _state.update { it.copy(isAutoLockEnabled = enabled) }
            }
        }
    }

    fun onNumberClick(num: String) {
        if (_state.value.enteredPin.length < 4) {
            val newPin = _state.value.enteredPin + num
            _state.update { 
                it.copy(
                    enteredPin = newPin,
                    isError = false
                )
            }
            
            // Auto unlock/set if 4 digits entered
            if (newPin.length == 4) {
                onUnlockClick()
            }
        }
    }

    fun onDelete() {
        if (_state.value.enteredPin.isNotEmpty()) {
            _state.update { 
                it.copy(
                    enteredPin = it.enteredPin.dropLast(1),
                    isError = false
                )
            }
        }
    }

    fun onUnlockClick() {
        val enteredPin = _state.value.enteredPin
        if (enteredPin.length != 4) return

        viewModelScope.launch {
            if (_state.value.isFirstTime) {
                lockManager.savePin(enteredPin)
                _state.update { it.copy(isUnlocked = true) }
            } else {
                val enteredHash = lockManager.hashPin(enteredPin)
                if (enteredHash == _state.value.savedPinHash) {
                    _state.update { it.copy(isUnlocked = true) }
                } else {
                    _state.update { 
                        it.copy(
                            isError = true,
                            enteredPin = ""
                        )
                    }
                }
            }
        }
    }

    fun onBiometricSuccess() {
        _state.update { it.copy(isUnlocked = true) }
    }

    fun toggleAutoLock(enabled: Boolean) {
        viewModelScope.launch {
            lockManager.setAutoLock(enabled)
        }
    }
}
