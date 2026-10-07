package com.wandr.android.ui.activity

import android.content.res.Configuration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.wandr.android.R
import com.wandr.android.ui.theme.WandrTheme
import com.wandr.domain.geo.MapTile
import com.wandr.domain.geo.StaticMapLayout
import com.wandr.domain.model.GpsTrackpoint
import kotlin.math.roundToInt

/**
 * The route of an activity (or, while recording, the position so far) on an OpenStreetMap background. The map is static (no gestures): the route is fitted into the
 * view, the tiles that cover it are loaded (and cached) by Coil, the route is drawn above. Until the tiles are there
 * the route is drawn on a plain background. The attribution is required by the OSM license.
 */
@Composable
fun OsmTrackMap(
    trackpoints: List<GpsTrackpoint>,
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape
) {
    val routeColor = MaterialTheme.colorScheme.primary
    val startColor = Color(0xFF4CAF50)
    val endColor = MaterialTheme.colorScheme.error
    var size by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    val paddingPx = with(density) { 32.dp.toPx() }.toDouble()
    val layout = remember(trackpoints, size) {
        if (size.width > 0 && size.height > 0 && trackpoints.isNotEmpty()) {
            StaticMapLayout.fit(
                trackpoints,
                size.width.toDouble(),
                size.height.toDouble(),
                paddingPx
            )
        } else null
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .onSizeChanged { size = it }
    ) {
        layout?.tiles?.forEach { tile -> key(tile.zoom, tile.x, tile.y) { MapTileImage(tile) } }

        Canvas(Modifier.fillMaxSize()) {
            layout ?: return@Canvas
            val points = trackpoints.map { p ->
                layout.project(p.latitude, p.longitude)
                    .let { (x, y) -> Offset(x.toFloat(), y.toFloat()) }
            }
            if (points.size < 2) {
                // A single position (the recording before the first track point): just the dot.
                drawCircle(Color.White, radius = 15f, center = points.first())
                drawCircle(routeColor, radius = 11f, center = points.first())
                return@Canvas
            }
            val path = Path()
            points.forEachIndexed { index, o ->
                if (index == 0) path.moveTo(
                    o.x,
                    o.y
                ) else path.lineTo(o.x, o.y)
            }
            // A white outline keeps the route readable on every map color.
            drawPath(
                path,
                Color.White,
                style = Stroke(width = 13f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            drawPath(
                path,
                routeColor,
                style = Stroke(width = 8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            drawCircle(Color.White, radius = 15f, center = points.first())
            drawCircle(startColor, radius = 11f, center = points.first())
            drawCircle(Color.White, radius = 15f, center = points.last())
            drawCircle(endColor, radius = 11f, center = points.last())
        }

        if (layout != null) {
            Text(
                text = stringResource(R.string.map_attribution),
                style = MaterialTheme.typography.labelSmall,
                color = Color.Black.copy(alpha = 0.7f),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .background(Color.White.copy(alpha = 0.7f))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            )
        }
    }
}

@Composable
private fun MapTileImage(tile: MapTile) {
    val density = LocalDensity.current
    val tileDp = with(density) { StaticMapLayout.TILE_SIZE.toDp() }
    val context = LocalContext.current
    AsyncImage(
        model = tile.url,
        imageLoader = OsmTiles.loader(context),
        contentDescription = null,
        contentScale = ContentScale.FillBounds,
        modifier = Modifier
            .offset { IntOffset(tile.left.roundToInt(), tile.top.roundToInt()) }
            .size(tileDp)
    )
}

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun OsmTrackMapPreview() {
    WandrTheme {
        // Previews have no network: the route shows on the plain background.
        OsmTrackMap(
            trackpoints = listOf(
                GpsTrackpoint(47.3769, 8.5417, 400.0, 0L),
                GpsTrackpoint(47.3779, 8.5437, 410.0, 0L),
                GpsTrackpoint(47.3789, 8.5427, 420.0, 0L),
                GpsTrackpoint(47.3801, 8.5460, 420.0, 0L)
            ),
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        )
    }
}
