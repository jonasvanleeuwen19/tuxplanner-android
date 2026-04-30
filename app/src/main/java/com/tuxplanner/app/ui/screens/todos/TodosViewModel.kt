package com.tuxplanner.app.ui.screens.todos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuxplanner.app.data.model.TodoCreate
import com.tuxplanner.app.data.model.TodoResponse
import com.tuxplanner.app.data.model.TodoUpdate
import com.tuxplanner.app.data.repository.ApiResult
import com.tuxplanner.app.data.repository.TodoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class TodosUiState(
    val isLoading: Boolean = false,
    val todos: List<TodoResponse> = emptyList(),
    val error: String? = null
)

class TodosViewModel(private val todoRepository: TodoRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(TodosUiState())
    val uiState: StateFlow<TodosUiState> = _uiState

    init {
        loadTodos()
    }

    fun loadTodos() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = todoRepository.getTodos()) {
                is ApiResult.Success -> _uiState.value = TodosUiState(todos = result.data)
                is ApiResult.Error -> _uiState.value = TodosUiState(error = result.message)
            }
        }
    }

    fun createTodo(title: String, priority: String) {
        viewModelScope.launch {
            val todo = TodoCreate(title = title, priority = priority)
            when (val result = todoRepository.createTodo(todo)) {
                is ApiResult.Success -> {
                    val updated = listOf(result.data) + _uiState.value.todos
                    _uiState.value = _uiState.value.copy(todos = updated)
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
}
