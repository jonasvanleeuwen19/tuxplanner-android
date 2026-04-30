package com.tuxplanner.app.data.repository

import com.tuxplanner.app.data.model.EventCreate
import com.tuxplanner.app.data.model.EventResponse
import com.tuxplanner.app.data.model.EventUpdate
import com.tuxplanner.app.data.network.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class EventRepository(private val client: ApiClient) {

    suspend fun getEvents(start: String? = null, end: String? = null): ApiResult<List<EventResponse>> =
        withContext(Dispatchers.IO) {
            try {
                val response = client.getService().getEvents(start, end)
                if (response.isSuccessful) {
                    ApiResult.Success(response.body() ?: emptyList())
                } else {
                    ApiResult.Error(response.message(), response.code())
                }
            } catch (e: Exception) {
                ApiResult.Error(e.message ?: "Network error")
            }
        }

    suspend fun createEvent(event: EventCreate): ApiResult<EventResponse> =
        withContext(Dispatchers.IO) {
            try {
                val response = client.getService().createEvent(event)
                if (response.isSuccessful) {
                    ApiResult.Success(response.body()!!)
                } else {
                    ApiResult.Error(response.message(), response.code())
                }
            } catch (e: Exception) {
                ApiResult.Error(e.message ?: "Network error")
            }
        }

    suspend fun updateEvent(id: Int, update: EventUpdate): ApiResult<EventResponse> =
        withContext(Dispatchers.IO) {
            try {
                val response = client.getService().updateEvent(id, update)
                if (response.isSuccessful) {
                    ApiResult.Success(response.body()!!)
                } else {
                    ApiResult.Error(response.message(), response.code())
                }
            } catch (e: Exception) {
                ApiResult.Error(e.message ?: "Network error")
            }
        }

    suspend fun deleteEvent(id: Int): ApiResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = client.getService().deleteEvent(id)
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
