package com.tuxplanner.app.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuxplanner.app.data.model.EventResponse
import com.tuxplanner.app.data.model.TodoResponse
import com.tuxplanner.app.data.model.TodoUpdate
import com.tuxplanner.app.data.repository.ApiResult
import com.tuxplanner.app.data.repository.AuthRepository
import com.tuxplanner.app.data.repository.EventRepository
import com.tuxplanner.app.data.repository.TodoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class DashboardUiState(
    val isLoading: Boolean = false,
    val userName: String = "",
    val todayEvents: List<EventResponse> = emptyList(),
    val todayTodos: List<TodoResponse> = emptyList(),
    val workSessions: List<EventResponse> = emptyList(),
    val error: String? = null
)

class DashboardViewModel(
    private val eventRepository: EventRepository,
    private val todoRepository: TodoRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState

    private val isoFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    init {
        loadDashboard()
    }

    fun loadDashboard() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val today = LocalDate.now()
            val startStr = today.atStartOfDay().format(isoFormatter)
            val endStr = today.atTime(23, 59, 59).format(isoFormatter)

            val eventsResult = eventRepository.getEvents(startStr, endStr)
            val todosResult = todoRepository.getTodos()
            val meResult = authRepository.getMe()

            val userName = if (meResult is ApiResult.Success) meResult.data.username else ""

            val allTodayEvents = if (eventsResult is ApiResult.Success) eventsResult.data else emptyList()
            val todayEvents = allTodayEvents.filter { it.source != "task_session" }
            val workSessions = allTodayEvents.filter { it.source == "task_session" }

            val todayTodos = if (todosResult is ApiResult.Success) {
                todosResult.data.filter { todo ->
                    todo.dueDate?.startsWith(today.toString()) == true
                }
            } else emptyList()

            val error = when {
                eventsResult is ApiResult.Error -> eventsResult.message
                todosResult is ApiResult.Error -> todosResult.message
                else -> null
            }

            _uiState.value = DashboardUiState(
                isLoading = false,
                userName = userName,
                todayEvents = todayEvents,
                todayTodos = todayTodos,
                workSessions = workSessions,
                error = error
            )
        }
    }

    fun toggleTodoComplete(todo: TodoResponse) {
        viewModelScope.launch {
            val update = TodoUpdate(completed = !todo.completed)
            when (val result = todoRepository.updateTodo(todo.id, update)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        todayTodos = _uiState.value.todayTodos.map {
                            if (it.id == todo.id) result.data else it
                        }
                    )
                }
                is ApiResult.Error -> _uiState.value = _uiState.value.copy(error = result.message)
            }
        }
    }
}
