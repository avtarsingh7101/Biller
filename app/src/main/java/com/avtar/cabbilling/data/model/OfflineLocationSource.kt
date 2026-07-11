package com.avtar.cabbilling.data.model

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Loads the bundled offline city database (assets/in_cities.txt) once into
 * memory as raw "City|State" lines, pre-sorted case-insensitively by city name.
 *
 * Search is a binary-search prefix lookup, so it stays instant even with
 * hundreds of thousands of entries (no full scan, no background thread needed).
 * Falls back to the built-in [IndiaLocations] list if the asset is missing.
 */
object OfflineLocationSource {

    @Volatile
    private var cache: List<String>? = null

    suspend fun load(context: Context): List<String> {
        cache?.let { return it }
        return withContext(Dispatchers.IO) {
            val lines = runCatching {
                context.assets.open("in_cities.txt").bufferedReader().useLines { seq ->
                    seq.filter { it.isNotBlank() }.toList()
                }
            }.getOrDefault(emptyList())

            val result = lines.ifEmpty {
                IndiaLocations.all
                    .map { "${it.city}|${it.state}" }
                    .sortedBy { it.substringBefore('|').lowercase() }
            }
            cache = result
            result
        }
    }

    /**
     * Prefix search over the sorted lines: returns up to [limit] parsed matches
     * whose city name starts with [query] (case-insensitive).
     */
    fun filter(lines: List<String>, query: String, limit: Int = 60): List<IndiaLocation> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return lines.asSequence().take(limit).map(::parse).toList()

        // Leftmost index whose city key is >= q.
        var lo = 0
        var hi = lines.size
        while (lo < hi) {
            val mid = (lo + hi) ushr 1
            if (cityKey(lines[mid]) < q) lo = mid + 1 else hi = mid
        }

        val out = ArrayList<IndiaLocation>()
        var i = lo
        while (i < lines.size && out.size < limit) {
            if (!cityKey(lines[i]).startsWith(q)) break
            out.add(parse(lines[i]))
            i++
        }
        return out
    }

    private fun cityKey(line: String): String = line.substringBefore('|').lowercase()

    private fun parse(line: String): IndiaLocation {
        val idx = line.indexOf('|')
        return if (idx < 0) IndiaLocation(line.trim(), "")
        else IndiaLocation(line.substring(0, idx).trim(), line.substring(idx + 1).trim())
    }
}
