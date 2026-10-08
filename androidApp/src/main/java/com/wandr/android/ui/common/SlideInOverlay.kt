package com.wandr.android.ui.common

import android.content.res.Configuration
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wandr.android.ui.theme.WandrTheme
import kotlin.coroutines.cancellation.CancellationException

/**
 * Full-screen drill-down page that slides in from the right over everything below it (including a bottom
 * tab bar) and slides out again. Supports the predictive back gesture: while swiping, the page follows the
 * finger and shrinks slightly; releasing commits ([onBack]) or cancels and springs back.
 *
 * Show it by passing a non-null [item]; pass null to dismiss. The last non-null item is kept while the exit
 * animation runs, so [content] never sees a null.
 *
 * @param onBack called when the back gesture or button commits; the caller must then set the item to null
 */
@Composable
fun <T : Any> SlideInOverlay(
    item: T?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    /** Reports how far the page is shown (0 = closed, 1 = open) on every frame, also while the back gesture drags; used by [HeroLayer]. */
    onProgress: ((Float) -> Unit)? = null,
    content: @Composable (T) -> Unit
) {
    var lastItem by remember { mutableStateOf<T?>(null) }
    if (item != null) lastItem = item

    /** 1f = fully off-screen to the right, 0f = shown. The gesture adds a little drag on top. */
    val offset = remember { Animatable(1f) }
    /** 0f..1f progress of an ongoing predictive back gesture (drives the scale). */
    val backProgress = remember { Animatable(0f) }
    val visible = item != null
    val currentOnProgress by rememberUpdatedState(onProgress)
    var pageCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

    LaunchedEffect(Unit) {
        snapshotFlow { 1f - offset.value }.collect { currentOnProgress?.invoke(it) }
    }

    LaunchedEffect(visible) {
        offset.animateTo(if (visible) 0f else 1f, tween(durationMillis = 300, easing = FastOutSlowInEasing))
        backProgress.snapTo(0f)
        if (!visible) lastItem = null
    }

    PredictiveBackHandler(enabled = visible) { progress ->
        try {
            progress.collect { event ->
                backProgress.snapTo(event.progress)
                offset.snapTo(event.progress * MAX_DRAG_FRACTION)
            }
            onBack() // committed: the LaunchedEffect continues the slide-out from the current position
        } catch (cancelled: CancellationException) {
            // Gesture cancelled: spring back.
            offset.animateTo(0f, tween(durationMillis = 200))
            backProgress.animateTo(0f, tween(durationMillis = 200))
            throw cancelled
        }
    }

    val shown = lastItem
    if (shown != null && (visible || offset.value < 1f)) {
        Surface(
            modifier = modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = offset.value * size.width
                    val scale = 1f - SCALE_DELTA * backProgress.value
                    scaleX = scale
                    scaleY = scale
                    shadowElevation = 16.dp.toPx()
                }
                .clip(if (backProgress.value > 0f) MaterialTheme.shapes.extraLarge else RectangleShape)
                // Swallow touches so the page below does not react while this one is on top.
                .pointerInput(Unit) { detectTapGestures { } }
                .onGloballyPositioned { pageCoordinates = it },
            color = MaterialTheme.colorScheme.background
        ) {
            CompositionLocalProvider(LocalOverlayCoordinates provides { pageCoordinates }) {
                content(
                    shown
                )
            }
        }
    }
}

private const val MAX_DRAG_FRACTION = 0.25f
private const val SCALE_DELTA = 0.1f

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun SlideInOverlayPreview() {
    WandrTheme {
        Box(Modifier.fillMaxSize()) {
            Text("Underlying screen", modifier = Modifier.align(Alignment.Center))
            SlideInOverlay(item = "Team Details", onBack = {}) { title ->
                Box(Modifier
                    .fillMaxSize()
                    .padding(24.dp), contentAlignment = Alignment.Center) { Text(title) }
            }
        }
    }
}
