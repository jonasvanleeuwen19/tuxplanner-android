package com.tuxplanner.app.ui.screens.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuxplanner.app.data.model.CalendarListResponse
import com.tuxplanner.app.data.model.EventCreate
import com.tuxplanner.app.data.model.EventResponse
import com.tuxplanner.app.data.model.EventUpdate
import com.tuxplanner.app.data.repository.ApiResult
import com.tuxplanner.app.data.repository.CalendarListRepository
import com.tuxplanner.app.data.repository.EventRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class EventsUiState(
    val isLoading: Boolean = false,
    val events: List<EventResponse> = emptyList(),
    val calendarLists: List<CalendarListResponse> = emptyList(),
    val filterCalendarListId: Int? = null,
    val error: String? = null
)

class EventsViewModel(
    private val eventRepository: EventRepository,
    private val calendarListRepository: CalendarListRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EventsUiState())
    val uiState: StateFlow<EventsUiState> = _uiState

    init {
        loadEvents()
    }

    fun loadEvents() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val listsResult = calendarListRepository.getCalendarLists()
            val eventsResult = eventRepository.getEvents()
            val lists = if (listsResult is ApiResult.Success) listsResult.data else _uiState.value.calendarLists
            when (eventsResult) {
                is ApiResult.Success -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    events = eventsResult.data,
                    calendarLists = lists,
                    error = null
                )
                is ApiResult.Error -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    calendarLists = lists,
                    error = eventsResult.message
                )
            }
        }
    }

    fun createEvent(event: EventCreate) {
        viewModelScope.launch {
            when (val result = eventRepository.createEvent(event)) {
                is ApiResult.Success -> {
                    val updated = listOf(result.data) + _uiState.value.events
                    _uiState.value = _uiState.value.copy(events = updated)
                    loadEvents()
                }
                is ApiResult.Error -> _uiState.value = _uiState.value.copy(error = result.message)
            }
        }
    }

    fun updateEvent(id: Int, update: EventUpdate) {
        viewModelScope.launch {
            when (val result = eventRepository.updateEvent(id, update)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        events = _uiState.value.events.map {
                            if (it.id == id) result.data else it
                        }
                    )
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

    fun setCalendarListFilter(id: Int?) {
        _uiState.value = _uiState.value.copy(filterCalendarListId = id)
    }
}
