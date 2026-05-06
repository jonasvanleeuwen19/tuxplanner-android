package com.tuxplanner.app.data.repository

import com.tuxplanner.app.data.model.CalendarListCreate
import com.tuxplanner.app.data.model.CalendarListResponse
import com.tuxplanner.app.data.model.CalendarListUpdate
import com.tuxplanner.app.data.network.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CalendarListRepository(private val client: ApiClient) {

    suspend fun getCalendarLists(): ApiResult<List<CalendarListResponse>> =
        withContext(Dispatchers.IO) {
            try {
                val response = client.getService().getCalendarLists()
                if (response.isSuccessful) {
                    ApiResult.Success(response.body() ?: emptyList())
                } else {
                    ApiResult.Error(response.message(), response.code())
                }
            } catch (e: Exception) {
                ApiResult.Error(e.message ?: "Network error")
            }
        }

    suspend fun createCalendarList(body: CalendarListCreate): ApiResult<CalendarListResponse> =
        withContext(Dispatchers.IO) {
            try {
                val response = client.getService().createCalendarList(body)
                if (response.isSuccessful) {
                    ApiResult.Success(response.body()!!)
                } else {
                    ApiResult.Error(response.message(), response.code())
                }
            } catch (e: Exception) {
                ApiResult.Error(e.message ?: "Network error")
            }
        }

    suspend fun updateCalendarList(id: Int, body: CalendarListUpdate): ApiResult<CalendarListResponse> =
        withContext(Dispatchers.IO) {
            try {
                val response = client.getService().updateCalendarList(id, body)
                if (response.isSuccessful) {
                    ApiResult.Success(response.body()!!)
                } else {
                    ApiResult.Error(response.message(), response.code())
                }
            } catch (e: Exception) {
                ApiResult.Error(e.message ?: "Network error")
            }
        }

    suspend fun deleteCalendarList(id: Int): ApiResult<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val response = client.getService().deleteCalendarList(id)
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
