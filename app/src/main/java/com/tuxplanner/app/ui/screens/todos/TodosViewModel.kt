package com.tuxplanner.app.ui.screens.todos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuxplanner.app.data.model.TaskSessionCreate
import com.tuxplanner.app.data.model.TaskSessionResponse
import com.tuxplanner.app.data.model.TodoCreate
import com.tuxplanner.app.data.model.TodoListResponse
import com.tuxplanner.app.data.model.TodoResponse
import com.tuxplanner.app.data.model.TodoUpdate
import com.tuxplanner.app.data.repository.ApiResult
import com.tuxplanner.app.data.repository.EventRepository
import com.tuxplanner.app.data.repository.TaskSessionRepository
import com.tuxplanner.app.data.repository.TodoListRepository
import com.tuxplanner.app.data.repository.TodoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class TodosUiState(
    val isLoading: Boolean = false,
    val todos: List<TodoResponse> = emptyList(),
    val todoLists: List<TodoListResponse> = emptyList(),
    val sessions: List<TaskSessionResponse> = emptyList(),
    val isLoadingSessions: Boolean = false,
    val filter: String = "all",
    val sort: String = "all",
    val selectedListId: Int? = null,
    val error: String? = null,
    val externalEventIds: Set<Int> = emptySet()
)

class TodosViewModel(
    private val todoRepository: TodoRepository,
    private val todoListRepository: TodoListRepository,
    private val taskSessionRepository: TaskSessionRepository,
    private val eventRepository: EventRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TodosUiState())
    val uiState: StateFlow<TodosUiState> = _uiState

    init {
        loadTodos()
    }

    fun loadTodos() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val listsResult = todoListRepository.getTodoLists()
            val todosResult = todoRepository.getTodos()
            val eventsResult = eventRepository.getEvents()
            val lists = if (listsResult is ApiResult.Success) listsResult.data else _uiState.value.todoLists
            val externalEventIds = if (eventsResult is ApiResult.Success) {
                eventsResult.data.filter { it.source == "ical" }.map { it.id }.toSet()
            } else {
                _uiState.value.externalEventIds
            }
            when (todosResult) {
                is ApiResult.Success -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    todos = todosResult.data,
                    todoLists = lists,
                    externalEventIds = externalEventIds,
                    error = null
                )
                is ApiResult.Error -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    todoLists = lists,
                    externalEventIds = externalEventIds,
                    error = todosResult.message
                )
            }
        }
    }

    fun createTodo(
        title: String,
        description: String?,
        priority: String,
        dueDate: String?,
        todoListId: Int?,
        eventId: Int?
    ) {
        viewModelScope.launch {
            val todo = TodoCreate(
                title = title,
                description = description?.ifBlank { null },
                priority = priority,
                dueDate = dueDate?.ifBlank { null },
                todoListId = todoListId,
                eventId = eventId
            )
            when (val result = todoRepository.createTodo(todo)) {
                is ApiResult.Success -> {
                    val updated = listOf(result.data) + _uiState.value.todos
                    _uiState.value = _uiState.value.copy(todos = updated)
                }
                is ApiResult.Error -> _uiState.value = _uiState.value.copy(error = result.message)
            }
        }
    }

    fun updateTodo(
        id: Int,
        title: String? = null,
        description: String? = null,
        priority: String? = null,
        dueDate: String? = null,
        todoListId: Int? = null,
        completed: Boolean? = null,
        eventId: Int? = null
    ) {
        viewModelScope.launch {
            val update = TodoUpdate(
                title = title,
                description = description,
                completed = completed,
                priority = priority,
                dueDate = dueDate,
                todoListId = todoListId,
                eventId = eventId
            )
            when (val result = todoRepository.updateTodo(id, update)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        todos = _uiState.value.todos.map {
                            if (it.id == id) result.data else it
                        }
                    )
                }
                is ApiResult.Error -> _uiState.value = _uiState.value.copy(error = result.message)
            }
        }
    }

    fun toggleComplete(todo: TodoResponse) {
        viewModelScope.launch {
            val update = TodoUpdate(completed = !todo.completed)
            when (val result = todoRepository.updateTodo(todo.id, update)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        todos = _uiState.value.todos.map {
                            if (it.id == todo.id) result.data else it
                        }
                    )
                }
                is ApiResult.Error -> _uiState.value = _uiState.value.copy(error = result.message)
            }
        }
    }

    fun deleteTodo(id: Int) {
        viewModelScope.launch {
            when (todoRepository.deleteTodo(id)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        todos = _uiState.value.todos.filter { it.id != id }
                    )
                }
                is ApiResult.Error -> loadTodos()
            }
        }
    }

    fun setFilter(filter: String) {
        _uiState.value = _uiState.value.copy(filter = filter)
    }

    fun setSort(sort: String) {
        _uiState.value = _uiState.value.copy(sort = sort)
    }

    fun setListFilter(listId: Int?) {
        _uiState.value = _uiState.value.copy(selectedListId = listId)
    }

    fun loadSessions(todoId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingSessions = true)
            when (val result = taskSessionRepository.getTaskSessions(todoId)) {
                is ApiResult.Success -> _uiState.value = _uiState.value.copy(
                    sessions = result.data,
                    isLoadingSessions = false
                )
                is ApiResult.Error -> _uiState.value = _uiState.value.copy(
                    sessions = emptyList(),
                    isLoadingSessions = false
                )
            }
        }
    }

    fun addSession(todoId: Int, start: String, end: String?, note: String?) {
        viewModelScope.launch {
            val body = TaskSessionCreate(start = start, end = end?.takeIf { it.isNotBlank() }, note = note?.takeIf { it.isNotBlank() })
            when (val result = taskSessionRepository.createTaskSession(todoId, body)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        sessions = _uiState.value.sessions + result.data
                    )
                }
                is ApiResult.Error -> _uiState.value = _uiState.value.copy(error = result.message)
            }
        }
    }

    fun deleteSession(todoId: Int, sessionId: Int) {
        viewModelScope.launch {
            when (taskSessionRepository.deleteTaskSession(todoId, sessionId)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        sessions = _uiState.value.sessions.filter { it.id != sessionId }
                    )
                }
                is ApiResult.Error -> loadSessions(todoId)
            }
        }
    }
}
