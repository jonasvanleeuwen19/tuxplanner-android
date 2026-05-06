package com.tuxplanner.app.data.network

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

/** Serialisable representation of an OkHttp [Cookie]. */
private data class SerializableCookie(
    val name: String,
    val value: String,
    val expiresAt: Long,
    val domain: String,
    val path: String,
    val secure: Boolean,
    val httpOnly: Boolean,
    val hostOnly: Boolean
)

private fun Cookie.toSerializable() = SerializableCookie(
    name = name,
    value = value,
    expiresAt = expiresAt,
    domain = domain,
    path = path,
    secure = secure,
    httpOnly = httpOnly,
    hostOnly = hostOnly
)

private fun SerializableCookie.toCookie(): Cookie = Cookie.Builder()
    .name(name)
    .value(value)
    .apply {
        if (expiresAt != Long.MAX_VALUE) expiresAt(expiresAt)
        if (hostOnly) hostOnlyDomain(domain) else domain(domain)
        if (secure) secure()
        if (httpOnly) httpOnly()
    }
    .path(path)
    .build()

/**
 * An OkHttp [CookieJar] that persists cookies to [SharedPreferences][android.content.SharedPreferences]
 * so that a valid session survives process restarts.
 *
 * Cookies are stored per host, keyed by hostname, as a JSON array.
 * Expired cookies are silently discarded on read and on write.
 */
class PersistentCookieJar(context: Context) : CookieJar {

    private val prefs = context.getSharedPreferences("okhttp_cookies", Context.MODE_PRIVATE)
    private val gson = Gson()

    // In-memory cache: host → mutable list of serialisable cookies
    private val cache: MutableMap<String, MutableList<SerializableCookie>> = mutableMapOf()

    init {
        val listType = object : TypeToken<List<SerializableCookie>>() {}.type
        prefs.all.forEach { (host, raw) ->
            if (raw is String) {
                try {
                    val cookies: List<SerializableCookie> = gson.fromJson(raw, listType)
                    cache[host] = cookies.toMutableList()
                } catch (_: Exception) {
                    // Corrupt entry – skip and let it be overwritten on next save
                }
            }
        }
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        if (cookies.isEmpty()) return
        val host = url.host
        val bucket = cache.getOrPut(host) { mutableListOf() }
        for (cookie in cookies) {
            // Replace any existing cookie with the same name
            bucket.removeAll { it.name == cookie.name }
            bucket.add(cookie.toSerializable())
        }
        // Purge expired cookies
        val now = System.currentTimeMillis()
        bucket.removeAll { it.expiresAt != Long.MAX_VALUE && it.expiresAt < now }
        cache[host] = bucket
        prefs.edit().putString(host, gson.toJson(bucket)).apply()
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val host = url.host
        val bucket = cache[host] ?: return emptyList()
        val now = System.currentTimeMillis()
        return bucket
            .filter { it.expiresAt == Long.MAX_VALUE || it.expiresAt > now }
            .map { it.toCookie() }
    }

    /** Removes all persisted and in-memory cookies (call on logout). */
    fun clearAll() {
        cache.clear()
        prefs.edit().clear().apply()
    }
}
