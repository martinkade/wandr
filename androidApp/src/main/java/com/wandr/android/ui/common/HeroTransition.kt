package com.wandr.android.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt

/**
 * Hero (shared element) transition between a list item and a detail page that is shown in a [SlideInOverlay]: a small
 * element of the item (e.g. the challenge badge) flies to its place on the detail page while the page slides in, and flies
 * back when it slides out again, also while the predictive back gesture is dragged.
 *
 * How it works: the item marks the element with [heroSource], the page with [heroTarget]; tapping the item calls [begin];
 * the overlay reports how far it is shown ([onProgress], 0 = closed, 1 = open); [HeroLayer] draws the element between the
 * two places, and the real elements stay invisible while it flies, so there is always exactly one on screen.
 */
@Stable
class HeroTransitionState {
    /** The element that flies (its key), from [begin] until the next [begin] or [clear]. */
    var activeKey: String? by mutableStateOf(null)
        private set

    /** How the flying element looks, for a given size. */
    var content: (@Composable (Dp) -> Unit)? by mutableStateOf(null)
        private set

    /** How far the page is shown: 0 = closed, 1 = fully open. */
    var progress: Float by mutableFloatStateOf(0f)
        private set

    internal var rootCoordinates: LayoutCoordinates? = null
    private val sourceCoordinates = HashMap<String, LayoutCoordinates>()
    private val targetCoordinates = HashMap<String, LayoutCoordinates>()
    private var overlayCoordinates: (() -> LayoutCoordinates?)? = null

    /** Where the element of the tapped item was, relative to the root, when the tap happened. */
    private var sourceRect: Rect? = null

    /** Call when the item is tapped, before the page is opened. */
    fun begin(key: String, content: @Composable (Dp) -> Unit) {
        activeKey = key
        this.content = content
        val root = rootCoordinates
        val source = sourceCoordinates[key]
        sourceRect = if (root != null && source != null && root.isAttached && source.isAttached) {
            Rect(
                root.localPositionOf(source, Offset.Zero),
                Size(source.size.width.toFloat(), source.size.height.toFloat())
            )
        } else null
    }

    /** Forget the active element, e.g. when the page is opened without a tap (a notification). */
    fun clear() {
        activeKey = null
        content = null
        sourceRect = null
    }

    fun onProgress(shown: Float) {
        progress = shown.coerceIn(0f, 1f)
    }

    internal fun registerSource(key: String, coordinates: LayoutCoordinates) {
        sourceCoordinates[key] = coordinates
    }

    internal fun registerTarget(
        key: String,
        coordinates: LayoutCoordinates,
        overlay: () -> LayoutCoordinates?
    ) {
        targetCoordinates[key] = coordinates
        overlayCoordinates = overlay
    }

    /** Where the element of the page is when the page is fully open, relative to the root. */
    private fun targetRect(key: String): Rect? {
        val target = targetCoordinates[key] ?: return null
        val overlay = overlayCoordinates?.invoke() ?: return null
        if (!target.isAttached || !overlay.isAttached) return null
        // Measured inside the page, so the page's slide offset does not matter; the open page starts at the root's origin.
        return Rect(
            overlay.localPositionOf(target, Offset.Zero),
            Size(target.size.width.toFloat(), target.size.height.toFloat())
        )
    }

    /** The place of the flying element right now, or null if [key] does not fly (nothing active, closed, open, or unknown places). */
    fun currentRect(key: String): Rect? {
        if (key != activeKey || progress <= 0f || progress >= 1f) return null
        val from = sourceRect ?: return null
        val to = targetRect(key) ?: return null
        return lerpRect(from, to, progress)
    }

    /** True while [key] flies; the real elements are hidden then. */
    fun isFlying(key: String): Boolean = currentRect(key) != null
}

/** The rectangle [fraction] of the way from [from] to [to]. */
internal fun lerpRect(from: Rect, to: Rect, fraction: Float): Rect = Rect(
    left = from.left + (to.left - from.left) * fraction,
    top = from.top + (to.top - from.top) * fraction,
    right = from.right + (to.right - from.right) * fraction,
    bottom = from.bottom + (to.bottom - from.bottom) * fraction
)

val LocalHeroTransition = compositionLocalOf { HeroTransitionState() }

/** The coordinates of the sliding page the content is in (set by [SlideInOverlay]). */
internal val LocalOverlayCoordinates = compositionLocalOf<() -> LayoutCoordinates?> { { null } }

/** Marks the element of a list item that flies to the page. Invisible while it flies. */
@Composable
fun Modifier.heroSource(key: String): Modifier {
    val state = LocalHeroTransition.current
    return this
        .onGloballyPositioned { state.registerSource(key, it) }
        .graphicsLayer { alpha = if (state.isFlying(key)) 0f else 1f }
}

/** Marks the element on the page that the flying element lands on. Invisible while it flies. */
@Composable
fun Modifier.heroTarget(key: String): Modifier {
    val state = LocalHeroTransition.current
    val overlay = LocalOverlayCoordinates.current
    return this
        .onGloballyPositioned { state.registerTarget(key, it, overlay) }
        .graphicsLayer { alpha = if (state.isFlying(key)) 0f else 1f }
}

/** Draws the flying element above everything else. Put it last in the root layout that also holds the overlays. */
@Composable
fun HeroLayer(state: HeroTransitionState, modifier: Modifier = Modifier) {
    Box(modifier
        .fillMaxSize()
        .onGloballyPositioned { state.rootCoordinates = it }) {
        val key = state.activeKey ?: return@Box
        val rect = state.currentRect(key) ?: return@Box
        val content = state.content ?: return@Box
        val density = LocalDensity.current
        Box(Modifier.offset { IntOffset(rect.left.roundToInt(), rect.top.roundToInt()) }) {
            content(with(density) { rect.width.toDp() })
        }
    }
}
