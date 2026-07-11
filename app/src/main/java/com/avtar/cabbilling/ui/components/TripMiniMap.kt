package com.avtar.cabbilling.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.avtar.cabbilling.util.GeoPoint
import com.avtar.cabbilling.util.TripPath

/**
 * Offline route-trace map: plots the recorded GPS path with a green start marker and
 * a red end marker on a light grid. No tiles, no network, no third party — the drawn
 * shape is the actual route, which also makes a faked distance obvious at a glance.
 */
@Composable
fun TripMiniMap(
    path: List<GeoPoint>,
    modifier: Modifier = Modifier
) {
    val routeColor = MaterialTheme.colorScheme.primary
    val startColor = Color(0xFF2E7D32)
    val endColor = MaterialTheme.colorScheme.error
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
    val mapBg = MaterialTheme.colorScheme.surfaceContainerHighest
    val onBg = MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(mapBg)
    ) {
        Canvas(Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            val step = 26.dp.toPx()

            var gx = step
            while (gx < w) {
                drawLine(gridColor, Offset(gx, 0f), Offset(gx, h), 1f)
                gx += step
            }
            var gy = step
            while (gy < h) {
                drawLine(gridColor, Offset(0f, gy), Offset(w, gy), 1f)
                gy += step
            }

            val pts = TripPath.project(path, w, h, 22.dp.toPx())
            if (pts.size >= 2) {
                val line = Path().apply {
                    moveTo(pts.first().x, pts.first().y)
                    for (i in 1 until pts.size) lineTo(pts[i].x, pts[i].y)
                }
                drawPath(
                    line,
                    routeColor,
                    style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
            if (pts.isNotEmpty()) {
                val start = pts.first()
                drawCircle(startColor, 8.dp.toPx(), Offset(start.x, start.y))
                drawCircle(Color.White, 4.dp.toPx(), Offset(start.x, start.y))
                if (pts.size > 1) {
                    val end = pts.last()
                    drawCircle(endColor, 8.dp.toPx(), Offset(end.x, end.y))
                    drawCircle(Color.White, 4.dp.toPx(), Offset(end.x, end.y))
                }
            }
        }

        if (path.isEmpty()) {
            Text(
                "Route not recorded",
                style = MaterialTheme.typography.bodySmall,
                color = onBg,
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendDot(startColor)
                Spacer(Modifier.width(4.dp))
                Text("Start", style = MaterialTheme.typography.labelSmall, color = onBg)
                Spacer(Modifier.width(10.dp))
                LegendDot(endColor)
                Spacer(Modifier.width(4.dp))
                Text("End", style = MaterialTheme.typography.labelSmall, color = onBg)
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color) {
    Box(Modifier.size(8.dp).background(color, CircleShape))
}
