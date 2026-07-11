package com.avtar.cabbilling.data.model

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Live place search via Photon (photon.komoot.io) — a free, key-less
 * OpenStreetMap geocoder built for autocomplete. Results are biased to India
 * and filtered to Indian places. Any failure (no signal, timeout, bad response)
 * resolves to an empty list so the caller silently falls back to offline data.
 */
object OnlineLocationSearch {

    private const val ENDPOINT = "https://photon.komoot.io/api/"
    // Rough geographic centre of India, used to bias autocomplete ranking.
    private const val BIAS_LAT = 22.35
    private const val BIAS_LON = 78.66

    suspend fun search(query: String, limit: Int = 10): List<IndiaLocation> =
        withContext(Dispatchers.IO) {
            runCatching {
                val q = URLEncoder.encode(query.trim(), "UTF-8")
                val url = URL("$ENDPOINT?q=$q&limit=$limit&lang=en&lat=$BIAS_LAT&lon=$BIAS_LON")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 6_000
                    readTimeout = 6_000
                    setRequestProperty("User-Agent", "CabBilling/1.0 (offline cab billing app)")
                }
                try {
                    if (conn.responseCode != HttpURLConnection.HTTP_OK) return@runCatching emptyList<IndiaLocation>()
                    parse(conn.inputStream.bufferedReader().use { it.readText() })
                } finally {
                    conn.disconnect()
                }
            }.getOrDefault(emptyList())
        }

    private fun parse(body: String): List<IndiaLocation> {
        val features = JSONObject(body).optJSONArray("features") ?: return emptyList()
        val out = ArrayList<IndiaLocation>()
        val seen = HashSet<String>()
        for (i in 0 until features.length()) {
            val props = features.getJSONObject(i).optJSONObject("properties") ?: continue
            if (!props.optString("countrycode").equals("IN", ignoreCase = true)) continue

            val name = props.optString("name").trim()
            if (name.isEmpty()) continue
            var region = props.optString("state").trim()
            if (region.isEmpty()) region = props.optString("county").trim()

            val key = "$name|$region".lowercase()
            if (seen.add(key)) out.add(IndiaLocation(name, region))
        }
        return out
    }
}
