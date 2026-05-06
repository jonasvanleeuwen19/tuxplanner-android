package com.tuxplanner.app.data.repository

import com.tuxplanner.app.data.model.TodoListCreate
import com.tuxplanner.app.data.model.TodoListResponse
import com.tuxplanner.app.data.model.TodoListUpdate
import com.tuxplanner.app.data.network.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TodoListRepository(private val client: ApiClient) {

    suspend fun getTodoLists(): ApiResult<List<TodoListResponse>> =
        withContext(Dispatchers.IO) {
            try {
                val response = client.getService().getTodoLists()
                if (response.isSuccessful) {
                    ApiResult.Success(response.body() ?: emptyList())
                } else {
                    ApiResult.Error(response.message(), response.code())
                }
            } catch (e: Exception) {
                ApiResult.Error(e.message ?: "Network error")
            }
        }

    suspend fun createTodoList(body: TodoListCreate): ApiResult<TodoListResponse> =
        withContext(Dispatchers.IO) {
            try {
                val response = client.getService().createTodoList(body)
                if (response.isSuccessful) {
                    ApiResult.Success(response.body()!!)
                } else {
                    ApiResult.Error(response.message(), response.code())
                }
            } catch (e: Exception) {
                ApiResult.Error(e.message ?: "Network error")
            }
        }

    suspend fun updateTodoList(id: Int, body: TodoListUpdate): ApiResult<TodoListResponse> =
        withContext(Dispatchers.IO) {
            try {
                val response = client.getService().updateTodoList(id, body)
                if (response.isSuccessful) {
                    ApiResult.Success(response.body()!!)
                } else {
                    ApiResult.Error(response.message(), response.code())
                }
            } catch (e: Exception) {
                ApiResult.Error(e.message ?: "Network error")
            }
        }

    suspend fun deleteTodoList(id: Int): ApiResult<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val response = client.getService().deleteTodoList(id)
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
