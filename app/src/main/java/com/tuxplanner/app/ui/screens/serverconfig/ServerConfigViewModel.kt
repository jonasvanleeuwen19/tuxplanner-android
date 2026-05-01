package com.tuxplanner.app.ui.screens.serverconfig

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuxplanner.app.data.network.ApiClient
import com.tuxplanner.app.data.preferences.AppPreferences
import com.tuxplanner.app.ui.common.validateHost
import com.tuxplanner.app.ui.common.validatePort
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ServerConfigUiState(
    val host: String = AppPreferences.DEFAULT_HOST,
    val port: String = AppPreferences.DEFAULT_PORT,
    val hostError: String? = null,
    val portError: String? = null,
    val isSaving: Boolean = false,
    val savedMessage: String? = null
)

class ServerConfigViewModel(
    private val preferences: AppPreferences,
    private val apiClient: ApiClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(ServerConfigUiState())
    val uiState: StateFlow<ServerConfigUiState> = _uiState

    init {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                host = preferences.getHost(),
                port = preferences.getPort()
            )
        }
    }

    fun saveConfig(host: String, port: String, onSaved: () -> Unit) {
        val hostTrimmed = host.trim()
        val portTrimmed = port.trim()

        val hostError = validateHost(hostTrimmed)
        val portError = validatePort(portTrimmed)

        _uiState.value = _uiState.value.copy(
            hostError = hostError,
            portError = portError
        )

        if (hostError != null || portError != null) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            preferences.setHost(hostTrimmed)
            preferences.setPort(portTrimmed)
            apiClient.invalidate()
            _uiState.value = _uiState.value.copy(
                host = hostTrimmed,
                port = portTrimmed,
                isSaving = false,
                savedMessage = "Saved"
            )
            onSaved()
        }
    }
}

