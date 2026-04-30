package com.tuxplanner.app.ui.screens.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuxplanner.app.data.model.EventCreate
import com.tuxplanner.app.data.model.EventResponse
import com.tuxplanner.app.data.repository.ApiResult
import com.tuxplanner.app.data.repository.EventRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class EventsUiState(
    val isLoading: Boolean = false,
    val events: List<EventResponse> = emptyList(),
    val error: String? = null
)

class EventsViewModel(private val eventRepository: EventRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(EventsUiState())
    val uiState: StateFlow<EventsUiState> = _uiState

    private val isoFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    init {
        loadEvents()
    }

    fun loadEvents() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = eventRepository.getEvents()) {
                is ApiResult.Success -> _uiState.value = EventsUiState(events = result.data)
                is ApiResult.Error -> _uiState.value = EventsUiState(error = result.message)
            }
        }
    }

    fun createEvent(title: String, start: LocalDateTime, end: LocalDateTime?) {
        viewModelScope.launch {
            val event = EventCreate(
                title = title,
                start = start.format(isoFormatter),
                end = end?.format(isoFormatter)
            )
            when (val result = eventRepository.createEvent(event)) {
                is ApiResult.Success -> {
                    val updated = _uiState.value.events.toMutableList()
                    updated.add(0, result.data)
                    _uiState.value = _uiState.value.copy(events = updated)
                    loadEvents() // refresh to get server-sorted order
                }
                is ApiResult.Error -> _uiState.value = _uiState.value.copy(error = result.message)
            }
        }
    }

    fun deleteEvent(id: Int) {
        viewModelScope.launch {
            when (eventRepository.deleteEvent(id)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        events = _uiState.value.events.filter { it.id != id }
                    )
                }
                is ApiResult.Error -> loadEvents()
            }
        }
    }
}
