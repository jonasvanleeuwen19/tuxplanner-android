package com.tuxplanner.app.data.repository

import com.tuxplanner.app.data.model.MessageResponse
import com.tuxplanner.app.data.model.SetupRequest
import com.tuxplanner.app.data.model.SetupStatusResponse
import com.tuxplanner.app.data.model.UserInfo
import com.tuxplanner.app.data.network.ApiClient
import com.tuxplanner.app.data.preferences.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val message: String, val code: Int? = null) : ApiResult<Nothing>()
}

/** Build a user-visible error string that includes the HTTP status code, message, and body. */
private fun <T> httpError(response: retrofit2.Response<T>): ApiResult.Error {
    val body = try { response.errorBody()?.string()?.trim() } catch (_: Exception) { null }
    val detail = if (!body.isNullOrBlank()) "\n$body" else ""
    return ApiResult.Error("HTTP ${response.code()} ${response.message()}$detail", response.code())
}

class AuthRepository(
    private val client: ApiClient,
    private val preferences: AppPreferences
) {
    suspend fun getSetupStatus(): ApiResult<SetupStatusResponse> = withContext(Dispatchers.IO) {
        try {
            val response = client.getService().getSetupStatus()
            if (response.isSuccessful) {
                ApiResult.Success(response.body() ?: SetupStatusResponse())
            } else {
                httpError(response)
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun setup(username: String, password: String): ApiResult<MessageResponse> =
        withContext(Dispatchers.IO) {
            try {
                val response = client.getService().setup(SetupRequest(username, password))
                if (response.isSuccessful) {
                    preferences.setLoggedIn(true)
                    ApiResult.Success(response.body() ?: MessageResponse())
                } else {
                    httpError(response)
                }
            } catch (e: Exception) {
                ApiResult.Error(e.message ?: "Network error")
            }
        }

    suspend fun login(username: String, password: String): ApiResult<MessageResponse> =
        withContext(Dispatchers.IO) {
            try {
                val response = client.getService().login(username, password)
                if (response.isSuccessful) {
                    preferences.setLoggedIn(true)
                    ApiResult.Success(response.body() ?: MessageResponse())
                } else {
                    httpError(response)
                }
            } catch (e: Exception) {
                ApiResult.Error(e.message ?: "Network error")
            }
        }

    suspend fun logout(): ApiResult<MessageResponse> = withContext(Dispatchers.IO) {
        try {
            val response = client.getService().logout()
            preferences.setLoggedIn(false)
            client.clearCookies()
            if (response.isSuccessful) {
                ApiResult.Success(response.body() ?: MessageResponse())
            } else {
                httpError(response)
            }
        } catch (e: Exception) {
            preferences.setLoggedIn(false)
            client.clearCookies()
            ApiResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun getMe(): ApiResult<UserInfo> = withContext(Dispatchers.IO) {
        try {
            val response = client.getService().getMe()
            if (response.isSuccessful) {
                ApiResult.Success(response.body() ?: UserInfo())
            } else {
                httpError(response)
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }
}
