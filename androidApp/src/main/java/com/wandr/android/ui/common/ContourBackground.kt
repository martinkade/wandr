package com.wandr.android.ui.common

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wandr.android.ui.theme.WandrTheme
import com.wandr.domain.geo.ContourLines
import kotlin.math.roundToInt

/**
 * Elevation lines like on a topographic map as a quiet backdrop: the design language of WANDR for screens that are not
 * dominated by content (sign in, sign up, empty states). The lines are drawn behind [content] in a faint tone of the
 * text color, so they work in light and dark mode; every fifth line is a bit stronger, like the index contours of a map.
 */
@Composable
fun ContourBackground(
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.Center,
    lineColor: Color = MaterialTheme.colorScheme.onBackground,
    seed: Int = 7,
    content: @Composable () -> Unit = {}
) {
    Box(
        modifier
            .background(MaterialTheme.colorScheme.background)
            .drawWithCache {
                // The terrain depends on the shape of the area only (rounded, so a small resize does not rebuild it).
                val aspect = ((size.width / size.height) * ASPECT_STEPS).roundToInt() / ASPECT_STEPS
                val contours = ContourLines.generate(aspect.coerceAtLeast(0.2f), seed = seed)
                val regular = Path()
                val index = Path()
                for (i in 0 until contours.segmentCount) {
                    val path = if (contours.levels[i] % INDEX_EVERY == 0) index else regular
                    path.moveTo(
                        contours.segments[i * 4] * size.width,
                        contours.segments[i * 4 + 1] * size.height
                    )
                    path.lineTo(
                        contours.segments[i * 4 + 2] * size.width,
                        contours.segments[i * 4 + 3] * size.height
                    )
                }
                val thin = Stroke(width = 0.8.dp.toPx())
                val strong = Stroke(width = 1.4.dp.toPx())
                onDrawBehind {
                    drawPath(regular, lineColor.copy(alpha = REGULAR_ALPHA), style = thin)
                    drawPath(index, lineColor.copy(alpha = INDEX_ALPHA), style = strong)
                }
            }
            .padding(8.dp),
        contentAlignment = contentAlignment,
    ) {
        content()
    }
}

private const val ASPECT_STEPS = 10f
private const val INDEX_EVERY = 5
private const val REGULAR_ALPHA = 0.10f
private const val INDEX_ALPHA = 0.22f

@Preview(name = "Light Mode", showBackground = true, widthDp = 360, heightDp = 720)
@Preview(
    name = "Dark Mode",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    widthDp = 360,
    heightDp = 720
)
@Preview(name = "Tablet", showBackground = true, widthDp = 840, heightDp = 600)
@Composable
private fun ContourBackgroundPreview() {
    WandrTheme {
        ContourBackground(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "WANDR",
                    style = MaterialTheme.typography.headlineLarge
                )
            }
        }
    }
}
