package com.wandr.android.ui.activity.recording

import android.content.res.Configuration
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wandr.android.ui.theme.WandrTheme
import kotlin.math.roundToInt

/**
 * A fancy animated horizontal compass bar tape showing moving direction (degrees and cardinal direction).
 * Smoothly slides as the user turns or moves.
 *
 * @param heading moving direction bearing in degrees (0..360)
 */
@Composable
fun CompassBar(
    heading: Float,
    modifier: Modifier = Modifier
) {
    val animatedHeading by animateFloatAsState(
        targetValue = heading,
        animationSpec = tween(durationMillis = 300),
        label = "compassHeading"
    )

    val normalizedDegrees = ((animatedHeading % 360f) + 360f) % 360f
    val currentCardinal = cardinalDirection(normalizedDegrees)

    val density = LocalDensity.current
    val pxPerDegree = with(density) { 4.dp.toPx() }

    val primaryGold = MaterialTheme.colorScheme.primary
    val tickColor = MaterialTheme.colorScheme.onBackground

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Heading Badge: e.g. "315° NW"
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${normalizedDegrees.roundToInt()}°",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = currentCardinal,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = primaryGold
                )
            }

            Spacer(Modifier.height(4.dp))

            // Center indicator arrow
            Canvas(modifier = Modifier.size(width = 12.dp, height = 6.dp)) {
                val arrow = Path().apply {
                    moveTo(size.width / 2f, size.height)
                    lineTo(0f, 0f)
                    lineTo(size.width, 0f)
                    close()
                }
                drawPath(arrow, color = primaryGold)
            }

            Spacer(Modifier.height(2.dp))

            // Compass Tape Canvas
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
            ) {
                val centerX = size.width / 2f
                val tapeCenterHeading = animatedHeading

                // Draw ticks from heading - 90° to heading + 90°
                val minDegree = (tapeCenterHeading - 90f).toInt()
                val maxDegree = (tapeCenterHeading + 90f).toInt()

                for (deg in minDegree..maxDegree) {
                    if (deg % 5 != 0) continue

                    val x = centerX + (deg - tapeCenterHeading) * pxPerDegree
                    if (x < 0f || x > size.width) continue

                    val degreeMod = ((deg % 360) + 360) % 360
                    val isCardinal = degreeMod % 90 == 0
                    val isInterCardinal = degreeMod % 45 == 0 && !isCardinal
                    val isMajorTick = degreeMod % 15 == 0

                    val tickHeight = when {
                        isCardinal -> 14f
                        isInterCardinal -> 10f
                        isMajorTick -> 7f
                        else -> 4f
                    }

                    val tickColor = when {
                        degreeMod == 0 -> primaryGold // North is Gold
                        isCardinal -> tickColor
                        isInterCardinal -> tickColor.copy(alpha = 0.8f)
                        else -> tickColor.copy(alpha = 0.4f)
                    }

                    val strokeWidth = if (isCardinal) 3f else 2f

                    drawLine(
                        color = tickColor,
                        start = Offset(x, 0f),
                        end = Offset(x, tickHeight),
                        strokeWidth = strokeWidth
                    )
                }
            }
        }
    }
}

private fun cardinalDirection(degrees: Float): String {
    val idx = ((((degrees + 22.5f) % 360f) / 45f).toInt() + 8) % 8
    return when (idx) {
        0 -> "N"
        1 -> "NE"
        2 -> "E"
        3 -> "SE"
        4 -> "S"
        5 -> "SW"
        6 -> "W"
        7 -> "NW"
        else -> "N"
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun CompassBarPreview() {
    WandrTheme {
        CompassBar(heading = 45f)
    }
}
