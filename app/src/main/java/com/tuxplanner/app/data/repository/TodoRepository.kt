package com.tuxplanner.app.data.repository

import com.tuxplanner.app.data.model.TodoCreate
import com.tuxplanner.app.data.model.TodoResponse
import com.tuxplanner.app.data.model.TodoUpdate
import com.tuxplanner.app.data.network.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TodoRepository(private val client: ApiClient) {

    suspend fun getTodos(todoListId: Int? = null, eventId: Int? = null): ApiResult<List<TodoResponse>> =
        withContext(Dispatchers.IO) {
            try {
                val response = client.getService().getTodos(todoListId, eventId)
                if (response.isSuccessful) {
                    ApiResult.Success(response.body() ?: emptyList())
                } else {
                    ApiResult.Error(response.message(), response.code())
                }
            } catch (e: Exception) {
                ApiResult.Error(e.message ?: "Network error")
            }
        }

    suspend fun createTodo(todo: TodoCreate): ApiResult<TodoResponse> =
        withContext(Dispatchers.IO) {
            try {
                val response = client.getService().createTodo(todo)
                if (response.isSuccessful) {
                    ApiResult.Success(response.body()!!)
                } else {
                    ApiResult.Error(response.message(), response.code())
                }
            } catch (e: Exception) {
                ApiResult.Error(e.message ?: "Network error")
            }
        }

    suspend fun updateTodo(id: Int, update: TodoUpdate): ApiResult<TodoResponse> =
        withContext(Dispatchers.IO) {
            try {
                val response = client.getService().updateTodo(id, update)
                if (response.isSuccessful) {
                    ApiResult.Success(response.body()!!)
                } else {
                    ApiResult.Error(response.message(), response.code())
                }
            } catch (e: Exception) {
                ApiResult.Error(e.message ?: "Network error")
            }
        }

    suspend fun deleteTodo(id: Int): ApiResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = client.getService().deleteTodo(id)
            if (response.isSuccessful || response.code() == 204) {
                ApiResult.Success(Unit)
            } else {
                ApiResult.Error(response.message(), response.code())
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }
}
