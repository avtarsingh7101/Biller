package com.avtar.cabbilling.util

import android.graphics.PointF
import java.util.Locale
import kotlin.math.cos
import kotlin.math.min

/** A single recorded GPS fix along a trip. */
data class GeoPoint(val lat: Double, val lng: Double)

/**
 * Serialises a recorded route and projects it onto a 2D canvas. Shared by the
 * on-screen mini-map (Compose Canvas) and the exported trip image (android Canvas)
 * so both draw the exact same shape.
 */
object TripPath {

    fun encode(points: List<GeoPoint>): String =
        points.joinToString(";") { String.format(Locale.US, "%.6f,%.6f", it.lat, it.lng) }

    fun decode(raw: String): List<GeoPoint> {
        if (raw.isBlank()) return emptyList()
        return raw.split(";").mapNotNull { seg ->
            val parts = seg.split(",")
            if (parts.size != 2) return@mapNotNull null
            val lat = parts[0].toDoubleOrNull()
            val lng = parts[1].toDoubleOrNull()
            if (lat != null && lng != null) GeoPoint(lat, lng) else null
        }
    }

    /**
     * Projects [points] into pixel coordinates inside a [w] x [h] box (inset by [pad]),
     * preserving the route's true aspect ratio — longitude is compressed by cos(latitude),
     * north points up — and centring the result. A single point (or no real movement)
     * collapses to the centre so its marker still renders.
     */
    fun project(points: List<GeoPoint>, w: Float, h: Float, pad: Float): List<PointF> {
        if (points.isEmpty()) return emptyList()

        val minLat = points.minOf { it.lat }
        val maxLat = points.maxOf { it.lat }
        val minLng = points.minOf { it.lng }
        val maxLng = points.maxOf { it.lng }
        val kx = cos(Math.toRadians((minLat + maxLat) / 2.0)).coerceAtLeast(0.01)

        // World coords: x east (compressed by kx), y increasing southward (north-up).
        val worldX = points.map { (it.lng - minLng) * kx }
        val worldY = points.map { (maxLat - it.lat) }

        val minX = worldX.minOrNull() ?: 0.0
        val maxX = worldX.maxOrNull() ?: 0.0
        val minY = worldY.minOrNull() ?: 0.0
        val maxY = worldY.maxOrNull() ?: 0.0
        val spanX = maxX - minX
        val spanY = maxY - minY

        val availW = (w - 2 * pad).coerceAtLeast(1f).toDouble()
        val availH = (h - 2 * pad).coerceAtLeast(1f).toDouble()

        if (spanX < 1e-7 && spanY < 1e-7) {
            return points.map { PointF(w / 2f, h / 2f) }
        }

        val scale = min(availW / spanX.coerceAtLeast(1e-7), availH / spanY.coerceAtLeast(1e-7))
        val offX = (w - spanX * scale) / 2.0
        val offY = (h - spanY * scale) / 2.0

        return points.indices.map { i ->
            PointF(
                (offX + (worldX[i] - minX) * scale).toFloat(),
                (offY + (worldY[i] - minY) * scale).toFloat()
            )
        }
    }
}
