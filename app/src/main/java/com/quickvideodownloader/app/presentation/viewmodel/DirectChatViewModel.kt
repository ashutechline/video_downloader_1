package com.quickvideodownloader.app.presentation.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quickvideodownloader.app.data.local.entity.RecentChatEntity
import com.quickvideodownloader.app.domain.repository.RecentChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class Country(
    val name: String,
    val code: String,
    val flag: String
)

data class ChatUiState(
    val phoneNumber: String = "",
    val message: String = "",
    val selectedCountry: Country = Country("India", "+91", "🇮🇳"),
    val countries: List<Country> = listOf(
        Country("India", "+91", "🇮🇳"),
        Country("United States", "+1", "🇺🇸"),
        Country("United Kingdom", "+44", "🇬🇧"),
        Country("United Arab Emirates", "+971", "🇦🇪"),
        Country("Canada", "+1", "🇨🇦"),
        Country("Australia", "+61", "🇦🇺"),
        Country("Germany", "+49", "🇩🇪"),
        Country("France", "+33", "🇫🇷"),
        Country("Italy", "+39", "🇮🇹"),
        Country("Japan", "+81", "🇯🇵")
    ),
    val recentChats: List<RecentChatEntity> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class DirectChatViewModel @Inject constructor(
    private val repository: RecentChatRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        loadRecentChats()
    }

    private fun loadRecentChats() {
        repository.getAllRecentChats()
            .onEach { chats ->
                _uiState.update { it.copy(recentChats = chats) }
            }
            .launchIn(viewModelScope)
    }

    fun updateNumber(number: String) {
        _uiState.update { it.copy(phoneNumber = number, error = null) }
    }

    fun updateMessage(message: String) {
        _uiState.update { it.copy(message = message) }
    }
    
    fun selectCountry(country: Country) {
        _uiState.update { it.copy(selectedCountry = country) }
    }

    fun startChat(context: Context) {
        val number = _uiState.value.phoneNumber.trim()
        val message = _uiState.value.message.trim()
        val country = _uiState.value.selectedCountry

        if (number.isEmpty()) {
            _uiState.update { it.copy(error = "Please enter phone number") }
            return
        }

        if (number.length < 8 || number.length > 12) {
            _uiState.update { it.copy(error = "Enter a valid phone number (8-12 digits)") }
            return
        }

        val fullNumber = "${country.code.replace("+", "")}$number"
        val url = "https://wa.me/$fullNumber?text=${Uri.encode(message)}"
        
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(url)
            intent.setPackage("com.whatsapp")
            context.startActivity(intent)
            
            // Save to recent chats
            saveRecentChat(number, message, country)
        } catch (e: Exception) {
            // WhatsApp not installed, try without package restriction or show toast
            try {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse(url)
                context.startActivity(intent)
                saveRecentChat(number, message, country)
            } catch (e2: Exception) {
                Toast.makeText(context, "WhatsApp is not installed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveRecentChat(number: String, message: String, country: Country) {
        viewModelScope.launch {
            val fullNumber = "${country.code} $number"
            repository.insertRecentChat(
                RecentChatEntity(
                    number = fullNumber,
                    message = message,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    fun clearAllChats() {
        viewModelScope.launch {
            repository.clearAllRecentChats()
        }
    }
    
    fun autofill(chat: RecentChatEntity) {
        val parts = chat.number.split(" ")
        if (parts.size >= 2) {
            val countryCode = parts[0]
            val country = _uiState.value.countries.find { it.code == countryCode } ?: _uiState.value.selectedCountry
            _uiState.update { it.copy(
                selectedCountry = country,
                phoneNumber = parts.subList(1, parts.size).joinToString("").replace(" ", ""),
                message = chat.message,
                error = null
            ) }
        } else {
            _uiState.update { it.copy(phoneNumber = chat.number, message = chat.message, error = null) }
        }
    }
}
