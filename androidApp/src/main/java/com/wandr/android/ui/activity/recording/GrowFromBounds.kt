package com.wandr.android.ui.activity.recording

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.wandr.android.ui.theme.WandrTheme

/**
 * Container transform: the content, laid out at the full size of the parent, is only visible through a window that grows
 * from [from] (the bounds of the small element, in the parent's coordinates) to the whole parent as [progress] goes from
 * 0 to 1, and shrinks back. The corners go from [fromCornerRadius] to square.
 */
@Composable
fun GrowFromBounds(
    progress: Float,
    from: Rect,
    modifier: Modifier = Modifier,
    fromCornerRadius: Float = 0f,
    content: @Composable () -> Unit
) {
    Box(
        modifier
            .fillMaxSize()
            .graphicsLayer { alpha = (progress * 3f).coerceIn(0f, 1f) }
            .clip(GrowingWindow(progress, from, fromCornerRadius))
    ) { content() }
}

private class GrowingWindow(
    private val progress: Float,
    private val from: Rect,
    private val fromCornerRadius: Float
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val p = progress.coerceIn(0f, 1f)
        val rect = Rect(
            left = from.left * (1 - p),
            top = from.top * (1 - p),
            right = from.right + (size.width - from.right) * p,
            bottom = from.bottom + (size.height - from.bottom) * p
        )
        val radius = fromCornerRadius * (1 - p)
        return Outline.Rounded(RoundRect(rect, CornerRadius(radius)))
    }
}

@Preview(name = "Light Mode", showBackground = true, widthDp = 300, heightDp = 400)
@Preview(
    name = "Dark Mode",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    widthDp = 300,
    heightDp = 400
)
@Composable
private fun GrowFromBoundsPreview() {
    WandrTheme {
        Box(Modifier.fillMaxSize()) {
            GrowFromBounds(
                progress = 0.5f,
                from = Rect(40f, 600f, 560f, 760f),
                fromCornerRadius = 48f
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Halfway", modifier = Modifier.padding(16.dp))
                }
            }
        }
    }
}
