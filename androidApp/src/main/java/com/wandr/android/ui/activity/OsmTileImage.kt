package com.wandr.android.ui.activity

import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import coil3.compose.AsyncImage
import com.wandr.domain.geo.MapTile
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * One map tile at its place in the view ([MapTile.left] / [MapTile.top]), [sizePx] pixels wide. Loaded and cached by the
 * tile loader (see [OsmTiles]). One pixel larger than needed, so no seams show between neighbours of a scaled map.
 */
@Composable
internal fun OsmTileImage(tile: MapTile, sizePx: Float) {
    val context = LocalContext.current
    val sizeDp = with(LocalDensity.current) { (ceil(sizePx) + 1f).toDp() }
    AsyncImage(
        model = tile.url,
        imageLoader = OsmTiles.loader(context),
        contentDescription = null,
        contentScale = ContentScale.FillBounds,
        modifier = Modifier
            .offset { IntOffset(tile.left.roundToInt(), tile.top.roundToInt()) }
            .size(sizeDp)
    )
}
