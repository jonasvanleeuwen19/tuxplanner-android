package com.tuxplanner.app.data.network

import android.content.Context
import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import com.tuxplanner.app.data.preferences.AppPreferences
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class ApiClient(private val preferences: AppPreferences, context: Context) {

    private val cookieJar = PersistentCookieJar(context)

    private val gson = GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .create()

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private var _service: TuxPlannerApiService? = null
    private var _currentBaseUrl: String = ""

    private fun buildService(baseUrl: String): TuxPlannerApiService {
        val normalized = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val client = OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
        return Retrofit.Builder()
            .baseUrl(normalized)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(TuxPlannerApiService::class.java)
    }

    @Synchronized
    fun getService(): TuxPlannerApiService {
        val baseUrl = runBlocking { preferences.getBaseUrl() }
        if (_service == null || _currentBaseUrl != baseUrl) {
            _service = buildService(baseUrl)
            _currentBaseUrl = baseUrl
        }
        return _service!!
    }

    /** Call after updating the base URL so the client is recreated. */
    @Synchronized
    fun invalidate() {
        _service = null
        _currentBaseUrl = ""
    }

    /** Clear stored cookies (called on logout). */
    fun clearCookies() {
        cookieJar.clearAll()
    }
}
