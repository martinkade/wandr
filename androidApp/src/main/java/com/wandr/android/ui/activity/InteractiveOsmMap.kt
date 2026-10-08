package com.wandr.android.ui.activity

import android.content.res.Configuration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.wandr.android.R
import com.wandr.android.ui.theme.WandrRouteBlue
import com.wandr.android.ui.theme.WandrTheme
import com.wandr.domain.geo.MapViewport
import com.wandr.domain.model.GpsTrackpoint
import kotlin.math.abs

/**
 * An OpenStreetMap the user can move: drag with one finger, pinch with two. It follows the current [position] with the
 * route so far drawn on it, until the user moves or zooms it; then a button brings it back to the position and the
 * default zoom.
 *
 * @param route the track so far (already smoothed by the recording)
 * @param position where the user is now, or null while unknown
 */
@Composable
fun InteractiveOsmMap(
    route: List<GpsTrackpoint>,
    position: GpsTrackpoint?,
    modifier: Modifier = Modifier
) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    var centerLatitude by remember { mutableDoubleStateOf(position?.latitude ?: 0.0) }
    var centerLongitude by remember { mutableDoubleStateOf(position?.longitude ?: 0.0) }
    var zoom by remember { mutableDoubleStateOf(DEFAULT_ZOOM) }
    var following by remember { mutableStateOf(true) }

    // While following, the map is centered on the position (every new fix moves it).
    LaunchedEffect(position, following) {
        if (following && position != null) {
            centerLatitude = position.latitude
            centerLongitude = position.longitude
        }
    }

    val viewport = if (size.width > 0 && size.height > 0) {
        MapViewport(centerLatitude, centerLongitude, zoom, size.width.toDouble(), size.height.toDouble())
    } else null
    val currentViewport by rememberUpdatedState(viewport)

    val startColor = Color(0xFF4CAF50)

    Box(
        modifier
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .onSizeChanged { size = it }
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, pinch, _ ->
                    val view = currentViewport ?: return@detectTransformGestures
                    // The user takes over: the map stops following the position.
                    following = false
                    var next = view
                    if (pinch != 1f) {
                        next = next.zoomedAround(MapViewport.zoomAfterPinch(view.zoom, pinch.toDouble()), centroid.x.toDouble(), centroid.y.toDouble())
                    }
                    next = next.panned(pan.x.toDouble(), pan.y.toDouble())
                    centerLatitude = next.centerLatitude
                    centerLongitude = next.centerLongitude
                    zoom = next.zoom
                }
            }
    ) {
        if (viewport != null) {
            viewport.tiles.forEach { tile ->
                androidx.compose.runtime.key(tile.zoom, tile.x, tile.y) { OsmTileImage(tile, viewport.tileSizePx.toFloat()) }
            }

            Canvas(Modifier.fillMaxSize()) {
                val points = route.map { p -> viewport.project(p.latitude, p.longitude).let { (x, y) -> Offset(x.toFloat(), y.toFloat()) } }
                if (points.size >= 2) {
                    val path = smoothedPath(points)
                    // A white outline keeps the route readable on every map color.
                    drawPath(path, Color.White, style = Stroke(width = 13f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    drawPath(path, WandrRouteBlue, style = Stroke(width = 8f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    drawCircle(Color.White, radius = 15f, center = points.first())
                    drawCircle(startColor, radius = 11f, center = points.first())
                }
                position?.let {
                    val (x, y) = viewport.project(it.latitude, it.longitude)
                    val center = Offset(x.toFloat(), y.toFloat())
                    drawCircle(Color.White, radius = 18f, center = center)
                    drawCircle(WandrRouteBlue, radius = 13f, center = center)
                }
            }

            Text(
                text = stringResource(R.string.map_attribution),
                style = MaterialTheme.typography.labelSmall,
                color = Color.Black.copy(alpha = 0.7f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 56.dp)
                    .background(Color.White.copy(alpha = 0.7f))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            )
        }

        // Back to the position and the default zoom, once the user has moved or zoomed the map.
        if (!following || abs(zoom - DEFAULT_ZOOM) > ZOOM_EPSILON) {
            IconButton(
                onClick = {
                    following = true
                    zoom = DEFAULT_ZOOM
                    position?.let {
                        centerLatitude = it.latitude
                        centerLongitude = it.longitude
                    }
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                Icon(painterResource(R.drawable.ic_my_location), contentDescription = stringResource(R.string.recording_recenter))
            }
        }
    }
}

/** Street level: about 1 km of the surroundings on a phone. */
private const val DEFAULT_ZOOM = 16.5
private const val ZOOM_EPSILON = 0.05

@Preview(name = "Light Mode", showBackground = true, heightDp = 500)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, heightDp = 500)
@Composable
private fun InteractiveOsmMapPreview() {
    val route = listOf(
        GpsTrackpoint(47.3769, 8.5417, 400.0, 0L),
        GpsTrackpoint(47.3774, 8.5424, 400.0, 0L),
        GpsTrackpoint(47.3779, 8.5437, 400.0, 0L),
        GpsTrackpoint(47.3786, 8.5441, 400.0, 0L)
    )
    WandrTheme {
        // Previews have no network: the route shows on the plain background.
        InteractiveOsmMap(route = route, position = route.last(), modifier = Modifier.fillMaxSize())
    }
}
