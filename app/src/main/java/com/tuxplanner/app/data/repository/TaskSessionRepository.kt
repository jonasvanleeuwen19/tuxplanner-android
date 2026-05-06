package com.tuxplanner.app.data.repository

import com.tuxplanner.app.data.model.TaskSessionCreate
import com.tuxplanner.app.data.model.TaskSessionResponse
import com.tuxplanner.app.data.model.TaskSessionUpdate
import com.tuxplanner.app.data.network.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TaskSessionRepository(private val client: ApiClient) {

    suspend fun getTaskSessions(todoId: Int): ApiResult<List<TaskSessionResponse>> =
        withContext(Dispatchers.IO) {
            try {
                val response = client.getService().getTaskSessions(todoId)
                if (response.isSuccessful) {
                    ApiResult.Success(response.body() ?: emptyList())
                } else {
                    ApiResult.Error(response.message(), response.code())
                }
            } catch (e: Exception) {
                ApiResult.Error(e.message ?: "Network error")
            }
        }

    suspend fun createTaskSession(todoId: Int, body: TaskSessionCreate): ApiResult<TaskSessionResponse> =
        withContext(Dispatchers.IO) {
            try {
                val response = client.getService().createTaskSession(todoId, body)
                if (response.isSuccessful) {
                    ApiResult.Success(response.body()!!)
                } else {
                    ApiResult.Error(response.message(), response.code())
                }
            } catch (e: Exception) {
                ApiResult.Error(e.message ?: "Network error")
            }
        }

    suspend fun updateTaskSession(
        todoId: Int,
        sessionId: Int,
        body: TaskSessionUpdate
    ): ApiResult<TaskSessionResponse> =
        withContext(Dispatchers.IO) {
            try {
                val response = client.getService().updateTaskSession(todoId, sessionId, body)
                if (response.isSuccessful) {
                    ApiResult.Success(response.body()!!)
                } else {
                    ApiResult.Error(response.message(), response.code())
                }
            } catch (e: Exception) {
                ApiResult.Error(e.message ?: "Network error")
            }
        }

    suspend fun deleteTaskSession(todoId: Int, sessionId: Int): ApiResult<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val response = client.getService().deleteTaskSession(todoId, sessionId)
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
