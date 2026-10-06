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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wandr.android.ui.theme.WandrTheme
import com.wandr.domain.model.GpsTrackpoint
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min

/**
 * The route of an activity drawn as a line (no map tiles, so it is cheap enough for every card of a list). The aspect
 * ratio of the route is kept; start and end are marked.
 */
@Composable
fun TrackMapView(
    trackpoints: List<GpsTrackpoint>,
    modifier: Modifier = Modifier.fillMaxWidth().height(200.dp),
    shape: Shape = RoundedCornerShape(12.dp)
) {
    val routeColor = MaterialTheme.colorScheme.primary
    val startColor = Color(0xFF4CAF50)
    val endColor = MaterialTheme.colorScheme.error

    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (trackpoints.size < 2) return@Canvas
            val padding = 32f

            val minLat = trackpoints.minOf { it.latitude }
            val maxLat = trackpoints.maxOf { it.latitude }
            val minLon = trackpoints.minOf { it.longitude }
            val maxLon = trackpoints.maxOf { it.longitude }

            // Longitude degrees get shorter towards the poles; without this the route would look stretched.
            val lonScale = cos((minLat + maxLat) / 2 * PI / 180.0)
            val widthUnits = ((maxLon - minLon) * lonScale).coerceAtLeast(1e-6)
            val heightUnits = (maxLat - minLat).coerceAtLeast(1e-6)
            val scale = min((size.width - 2 * padding) / widthUnits, (size.height - 2 * padding) / heightUnits).toFloat()
            // Centered in the free space.
            val offsetX = (size.width - (widthUnits * scale).toFloat()) / 2
            val offsetY = (size.height - (heightUnits * scale).toFloat()) / 2

            fun project(p: GpsTrackpoint) = Offset(
                x = offsetX + (((p.longitude - minLon) * lonScale) * scale).toFloat(),
                y = size.height - (offsetY + ((p.latitude - minLat) * scale).toFloat())
            )

            val path = Path()
            trackpoints.forEachIndexed { index, point ->
                val o = project(point)
                if (index == 0) path.moveTo(o.x, o.y) else path.lineTo(o.x, o.y)
            }
            drawPath(path, routeColor, style = Stroke(width = 8f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawCircle(startColor, radius = 11f, center = project(trackpoints.first()))
            drawCircle(endColor, radius = 11f, center = project(trackpoints.last()))
        }
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun TrackMapViewPreview() {
    WandrTheme {
        TrackMapView(
            trackpoints = listOf(
                GpsTrackpoint(47.3769, 8.5417, 400.0, 0L),
                GpsTrackpoint(47.3779, 8.5437, 410.0, 0L),
                GpsTrackpoint(47.3789, 8.5427, 420.0, 0L),
                GpsTrackpoint(47.3801, 8.5460, 420.0, 0L)
            )
        )
    }
}
