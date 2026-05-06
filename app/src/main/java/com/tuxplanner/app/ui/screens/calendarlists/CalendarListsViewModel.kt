package com.tuxplanner.app.ui.screens.calendarlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuxplanner.app.data.model.CalendarListCreate
import com.tuxplanner.app.data.model.CalendarListResponse
import com.tuxplanner.app.data.model.CalendarListUpdate
import com.tuxplanner.app.data.repository.ApiResult
import com.tuxplanner.app.data.repository.CalendarListRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class CalendarListsUiState(
    val isLoading: Boolean = false,
    val lists: List<CalendarListResponse> = emptyList(),
    val error: String? = null
)

class CalendarListsViewModel(private val repo: CalendarListRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarListsUiState())
    val uiState: StateFlow<CalendarListsUiState> = _uiState

    init {
        loadLists()
    }

    fun loadLists() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = repo.getCalendarLists()) {
                is ApiResult.Success -> _uiState.value = CalendarListsUiState(lists = result.data)
                is ApiResult.Error -> _uiState.value = CalendarListsUiState(error = result.message)
            }
        }
    }

    fun createList(name: String, color: String) {
        viewModelScope.launch {
            val body = CalendarListCreate(name = name.trim(), color = color.trim().ifBlank { "#3b82f6" })
            when (val result = repo.createCalendarList(body)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        lists = _uiState.value.lists + result.data
                    )
                }
                is ApiResult.Error -> _uiState.value = _uiState.value.copy(error = result.message)
            }
        }
    }

    fun updateList(id: Int, name: String, color: String) {
        viewModelScope.launch {
            val body = CalendarListUpdate(
                name = name.trim().ifBlank { null },
                color = color.trim().ifBlank { null }
            )
            when (val result = repo.updateCalendarList(id, body)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        lists = _uiState.value.lists.map {
                            if (it.id == id) result.data else it
                        }
                    )
                }
                is ApiResult.Error -> _uiState.value = _uiState.value.copy(error = result.message)
            }
        }
    }

    fun toggleVisibility(list: CalendarListResponse) {
        viewModelScope.launch {
            val body = CalendarListUpdate(isVisible = !list.isVisible)
            when (val result = repo.updateCalendarList(list.id, body)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        lists = _uiState.value.lists.map {
                            if (it.id == list.id) result.data else it
                        }
                    )
                }
                is ApiResult.Error -> _uiState.value = _uiState.value.copy(error = result.message)
            }
        }
    }

    fun deleteList(id: Int) {
        viewModelScope.launch {
            when (repo.deleteCalendarList(id)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        lists = _uiState.value.lists.filter { it.id != id }
                    )
                }
                is ApiResult.Error -> loadLists()
            }
        }
    }
}
