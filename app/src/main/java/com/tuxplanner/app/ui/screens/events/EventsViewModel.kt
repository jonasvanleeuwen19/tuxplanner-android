package com.tuxplanner.app.ui.screens.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuxplanner.app.data.model.CalendarListResponse
import com.tuxplanner.app.data.model.EventCreate
import com.tuxplanner.app.data.model.EventResponse
import com.tuxplanner.app.data.model.EventUpdate
import com.tuxplanner.app.data.model.TodoResponse
import com.tuxplanner.app.data.model.TodoUpdate
import com.tuxplanner.app.data.repository.ApiResult
import com.tuxplanner.app.data.repository.CalendarListRepository
import com.tuxplanner.app.data.repository.EventRepository
import com.tuxplanner.app.data.repository.TodoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

enum class CalendarViewType { Month, Week, List }

data class EventsUiState(
    val isLoading: Boolean = false,
    val events: List<EventResponse> = emptyList(),
    val calendarLists: List<CalendarListResponse> = emptyList(),
    val filterCalendarListId: Int? = null,
    val error: String? = null,
    val viewType: CalendarViewType = CalendarViewType.Month,
    val selectedDate: LocalDate = LocalDate.now(),
    val displayedYearMonth: YearMonth = YearMonth.now(),
    val displayedWeekStart: LocalDate = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
    val linkedTasks: List<TodoResponse> = emptyList(),
    val allTodos: List<TodoResponse> = emptyList()
)

class EventsViewModel(
    private val eventRepository: EventRepository,
    private val calendarListRepository: CalendarListRepository,
    private val todoRepository: TodoRepository
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
            val todosResult = todoRepository.getTodos()
            val lists = if (listsResult is ApiResult.Success) listsResult.data else _uiState.value.calendarLists
            val todos = if (todosResult is ApiResult.Success) todosResult.data else _uiState.value.allTodos
            when (eventsResult) {
                is ApiResult.Success -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    events = eventsResult.data,
                    calendarLists = lists,
                    allTodos = todos,
                    error = null
                )
                is ApiResult.Error -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    calendarLists = lists,
                    allTodos = todos,
                    error = eventsResult.message
                )
            }
        }
    }

    fun loadTasksForEvent(eventId: Int) {
        viewModelScope.launch {
            when (val result = todoRepository.getTodos(eventId = eventId)) {
                is ApiResult.Success -> _uiState.value = _uiState.value.copy(linkedTasks = result.data)
                is ApiResult.Error -> _uiState.value = _uiState.value.copy(linkedTasks = emptyList())
            }
        }
    }

    fun linkTaskToEvent(todoId: Int, eventId: Int) {
        viewModelScope.launch {
            when (val result = todoRepository.updateTodo(todoId, TodoUpdate(eventId = eventId))) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        allTodos = _uiState.value.allTodos.map { if (it.id == todoId) result.data else it }
                    )
                    loadTasksForEvent(eventId)
                    loadEvents()
                }
                is ApiResult.Error -> _uiState.value = _uiState.value.copy(error = result.message)
            }
        }
    }

    fun unlinkTask(todoId: Int, eventId: Int) {
        viewModelScope.launch {
            when (val result = todoRepository.updateTodo(todoId, TodoUpdate(eventId = null))) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        allTodos = _uiState.value.allTodos.map { if (it.id == todoId) result.data else it }
                    )
                    loadTasksForEvent(eventId)
                    loadEvents()
                }
                is ApiResult.Error -> _uiState.value = _uiState.value.copy(error = result.message)
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

    fun setViewType(type: CalendarViewType) {
        _uiState.value = _uiState.value.copy(viewType = type)
    }

    fun selectDate(date: LocalDate) {
        _uiState.value = _uiState.value.copy(selectedDate = date)
    }

    fun prevMonth() {
        val prev = _uiState.value.displayedYearMonth.minusMonths(1)
        _uiState.value = _uiState.value.copy(displayedYearMonth = prev)
    }

    fun nextMonth() {
        val next = _uiState.value.displayedYearMonth.plusMonths(1)
        _uiState.value = _uiState.value.copy(displayedYearMonth = next)
    }

    fun prevWeek() {
        val prev = _uiState.value.displayedWeekStart.minusWeeks(1)
        _uiState.value = _uiState.value.copy(displayedWeekStart = prev)
    }

    fun nextWeek() {
        val next = _uiState.value.displayedWeekStart.plusWeeks(1)
        _uiState.value = _uiState.value.copy(displayedWeekStart = next)
    }
}
