package com.tuxplanner.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "tuxplanner_prefs")

class AppPreferences(private val context: Context) {

    companion object {
        private val KEY_HOST = stringPreferencesKey("host")
        private val KEY_PORT = stringPreferencesKey("port")
        private val KEY_IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        const val DEFAULT_HOST = "10.0.2.2"
        const val DEFAULT_PORT = "8000"
    }

    val hostFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_HOST] ?: DEFAULT_HOST
    }

    val portFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_PORT] ?: DEFAULT_PORT
    }

    val baseUrlFlow: Flow<String> = context.dataStore.data.map { prefs ->
        val host = prefs[KEY_HOST] ?: DEFAULT_HOST
        val port = prefs[KEY_PORT] ?: DEFAULT_PORT
        "http://$host:$port"
    }

    val isLoggedInFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_IS_LOGGED_IN] ?: false
    }

    suspend fun getHost(): String =
        context.dataStore.data.map { it[KEY_HOST] ?: DEFAULT_HOST }.first()

    suspend fun setHost(host: String) {
        context.dataStore.edit { prefs -> prefs[KEY_HOST] = host }
    }

    suspend fun getPort(): String =
        context.dataStore.data.map { it[KEY_PORT] ?: DEFAULT_PORT }.first()

    suspend fun setPort(port: String) {
        context.dataStore.edit { prefs -> prefs[KEY_PORT] = port }
    }

    suspend fun getBaseUrl(): String {
        val data = context.dataStore.data.first()
        val host = data[KEY_HOST] ?: DEFAULT_HOST
        val port = data[KEY_PORT] ?: DEFAULT_PORT
        return "http://$host:$port"
    }

    /** Returns true once the user has explicitly saved a host/port. */
    suspend fun isServerConfigured(): Boolean =
        context.dataStore.data.map { it[KEY_HOST] != null }.first()

    suspend fun setLoggedIn(loggedIn: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_IS_LOGGED_IN] = loggedIn
        }
    }

    suspend fun isLoggedIn(): Boolean =
        context.dataStore.data.map { it[KEY_IS_LOGGED_IN] ?: false }.first()
}
