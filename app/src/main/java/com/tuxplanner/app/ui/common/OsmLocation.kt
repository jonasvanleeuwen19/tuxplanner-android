package com.tuxplanner.app.ui.common

import android.webkit.WebSettings
import android.webkit.WebView
import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.net.URLEncoder

data class OsmSuggestion(
    val displayName: String,
    val lat: Double,
    val lon: Double
)

object OsmLocationService {
    private val client = OkHttpClient()

    suspend fun search(query: String, limit: Int = 8): List<OsmSuggestion> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val encoded = URLEncoder.encode(query, "UTF-8")
        val request = Request.Builder()
            .url("https://nominatim.openstreetmap.org/search?format=jsonv2&q=$encoded&limit=$limit")
            .header("User-Agent", "tuxplanner-android/1.0 (android)")
            .build()
        runCatching {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                parseSuggestions(body)
            }
        }.getOrDefault(emptyList())
    }

    suspend fun geocode(query: String): OsmSuggestion? = search(query, 1).firstOrNull()

    private fun parseSuggestions(raw: String): List<OsmSuggestion> {
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                val item = arr.optJSONObject(i) ?: return@mapNotNull null
                val name = item.optString("display_name")
                val lat = item.optString("lat").toDoubleOrNull()
                val lon = item.optString("lon").toDoubleOrNull()
                if (name.isBlank() || lat == null || lon == null) null else OsmSuggestion(name, lat, lon)
            }
        }.getOrDefault(emptyList())
    }
}

@Composable
fun OsmMiniMap(locationText: String, modifier: Modifier = Modifier) {
    var suggestion by remember(locationText) { mutableStateOf<OsmSuggestion?>(null) }
    var loading by remember(locationText) { mutableStateOf(false) }

    LaunchedEffect(locationText) {
        if (locationText.isBlank()) return@LaunchedEffect
        loading = true
        suggestion = OsmLocationService.geocode(locationText)
        loading = false
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp),
        contentAlignment = Alignment.Center
    ) {
        val item = suggestion
        if (item != null) {
            val bbox = "${item.lon - 0.01},${item.lat - 0.01},${item.lon + 0.01},${item.lat + 0.01}"
            val mapUrl =
                "https://www.openstreetmap.org/export/embed.html?bbox=$bbox&layer=mapnik&marker=${item.lat},${item.lon}"
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = false
                        settings.cacheMode = WebSettings.LOAD_NO_CACHE
                        settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            settings.safeBrowsingEnabled = true
                        }
                        loadUrl(mapUrl)
                    }
                },
                update = { it.loadUrl(mapUrl) },
                modifier = Modifier.fillMaxSize()
            )
        } else if (loading) {
            CircularProgressIndicator()
        }
    }
}
