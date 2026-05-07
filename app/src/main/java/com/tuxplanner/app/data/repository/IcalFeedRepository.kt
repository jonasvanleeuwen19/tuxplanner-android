package com.tuxplanner.app.data.repository

import com.tuxplanner.app.data.model.IcalFeedCreate
import com.tuxplanner.app.data.model.IcalFeedResponse
import com.tuxplanner.app.data.model.IcalFeedUpdate
import com.tuxplanner.app.data.network.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class IcalFeedRepository(private val client: ApiClient) {

    suspend fun getFeeds(): ApiResult<List<IcalFeedResponse>> = withContext(Dispatchers.IO) {
        try {
            val response = client.getService().getIcalFeeds()
            if (response.isSuccessful) {
                ApiResult.Success(response.body() ?: emptyList())
            } else {
                ApiResult.Error(response.message(), response.code())
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun createFeed(body: IcalFeedCreate): ApiResult<IcalFeedResponse> = withContext(Dispatchers.IO) {
        try {
            val response = client.getService().createIcalFeed(body)
            if (response.isSuccessful) {
                ApiResult.Success(response.body()!!)
            } else {
                ApiResult.Error(response.message(), response.code())
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun updateFeed(id: Int, body: IcalFeedUpdate): ApiResult<IcalFeedResponse> = withContext(Dispatchers.IO) {
        try {
            val response = client.getService().updateIcalFeed(id, body)
            if (response.isSuccessful) {
                ApiResult.Success(response.body()!!)
            } else {
                ApiResult.Error(response.message(), response.code())
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun deleteFeed(id: Int): ApiResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = client.getService().deleteIcalFeed(id)
            if (response.isSuccessful || response.code() == 204) {
                ApiResult.Success(Unit)
            } else {
                ApiResult.Error(response.message(), response.code())
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun syncFeed(id: Int): ApiResult<IcalFeedResponse> = withContext(Dispatchers.IO) {
        try {
            val response = client.getService().syncIcalFeed(id)
            if (response.isSuccessful) {
                ApiResult.Success(response.body()!!)
            } else {
                ApiResult.Error(response.message(), response.code())
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }
}
