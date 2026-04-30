package com.tuxplanner.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuxplanner.app.data.network.ApiClient
import com.tuxplanner.app.data.preferences.AppPreferences
import com.tuxplanner.app.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val baseUrl: String = AppPreferences.DEFAULT_BASE_URL,
    val isSaving: Boolean = false,
    val savedMessage: String? = null
)

class SettingsViewModel(
    private val preferences: AppPreferences,
    private val apiClient: ApiClient,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState

    init {
        viewModelScope.launch {
            val url = preferences.getBaseUrl()
            _uiState.value = _uiState.value.copy(baseUrl = url)
        }
    }

    fun saveBaseUrl(url: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            preferences.setBaseUrl(url.trim())
            apiClient.invalidate()
            _uiState.value = _uiState.value.copy(
                baseUrl = url.trim(),
                isSaving = false,
                savedMessage = "Saved"
            )
        }
    }

    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onLoggedOut()
        }
    }
}
