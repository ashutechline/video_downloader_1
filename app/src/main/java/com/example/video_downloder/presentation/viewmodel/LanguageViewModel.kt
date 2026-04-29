package com.example.video_downloder.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.video_downloder.data.local.LanguagePreferences
import com.example.video_downloder.domain.model.LanguageItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

data class LanguageUiState(
    val selectedLanguageCode: String = "en",
    val searchQuery: String = "",
    val languages: List<LanguageItem> = emptyList(),
    val filteredLanguages: List<LanguageItem> = emptyList()
)

@HiltViewModel
class LanguageViewModel @Inject constructor(
    private val languagePreferences: LanguagePreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(LanguageUiState())
    val uiState: StateFlow<LanguageUiState> = _uiState.asStateFlow()

    private val allLanguages = listOf(
        LanguageItem("English", "English", "en"),
        LanguageItem("Hindi", "हिन्दी", "hi"),
        LanguageItem("Gujarati", "ગુજરાતી", "gu"),
        LanguageItem("Spanish", "Español", "es"),
        LanguageItem("French", "Français", "fr"),
        LanguageItem("German", "Deutsch", "de"),
        LanguageItem("Arabic", "العربية", "ar"),
        LanguageItem("Chinese", "中文", "zh"),
        LanguageItem("Portuguese", "Português", "pt"),
        LanguageItem("Japanese", "日本語", "ja"),
        LanguageItem("Russian", "Русский", "ru"),
        LanguageItem("Korean", "한국어", "ko"),
        LanguageItem("Turkish", "Türkçe", "tr"),
        LanguageItem("Italian", "Italiano", "it"),
        LanguageItem("Vietnamese", "Tiếng Việt", "vi")
    )

    init {
        viewModelScope.launch {
            val savedLanguage = languagePreferences.getLanguage.first() ?: "en"
            _uiState.value = _uiState.value.copy(
                selectedLanguageCode = savedLanguage,
                languages = allLanguages,
                filteredLanguages = allLanguages
            )
        }
    }

    fun onSearchQueryChange(query: String) {
        val filtered = if (query.isEmpty()) {
            allLanguages
        } else {
            allLanguages.filter {
                it.name.contains(query, ignoreCase = true) ||
                it.nativeName.contains(query, ignoreCase = true)
            }
        }
        _uiState.value = _uiState.value.copy(
            searchQuery = query,
            filteredLanguages = filtered
        )
    }

    fun selectLanguage(code: String) {
        _uiState.value = _uiState.value.copy(selectedLanguageCode = code)
    }

    fun saveLanguage(onComplete: () -> Unit) {
        viewModelScope.launch {
            languagePreferences.saveLanguage(_uiState.value.selectedLanguageCode)
            onComplete()
        }
    }
}
