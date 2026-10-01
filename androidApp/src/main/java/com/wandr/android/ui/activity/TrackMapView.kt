package com.wandr.android.ui.activity

import android.content.res.Configuration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wandr.domain.model.GpsTrackpoint

@Composable
fun TrackMapView(
    trackpoints: List<GpsTrackpoint>,
    modifier: Modifier = Modifier
) {
    val polylineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val padding = 32f

            if (trackpoints.size < 2) {
                return@Canvas
            }

            val minLat = trackpoints.minOf { it.latitude }
            val maxLat = trackpoints.maxOf { it.latitude }
            val minLon = trackpoints.minOf { it.longitude }
            val maxLon = trackpoints.maxOf { it.longitude }

            val latRange = (maxLat - minLat).coerceAtLeast(0.00001)
            val lonRange = (maxLon - minLon).coerceAtLeast(0.00001)

            val path = Path()
            trackpoints.forEachIndexed { index, tp ->
                val x = padding + ((tp.longitude - minLon) / lonRange * (width - 2 * padding)).toFloat()
                val y = height - (padding + ((tp.latitude - minLat) / latRange * (height - 2 * padding)).toFloat())
                
                if (index == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }

            drawPath(
                path = path,
                color = polylineColor,
                style = Stroke(width = 6f)
            )
        }
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun TrackMapViewPreview() {
    MaterialTheme {
        TrackMapView(
            trackpoints = listOf(
                GpsTrackpoint(47.3769, 8.5417, 400.0, 0L),
                GpsTrackpoint(47.3779, 8.5437, 410.0, 0L),
                GpsTrackpoint(47.3789, 8.5427, 420.0, 0L)
            )
        )
    }
}
