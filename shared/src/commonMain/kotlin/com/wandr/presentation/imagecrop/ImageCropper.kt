package com.wandr.presentation.imagecrop

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Crop area as fractions (0..1) of the orientation-corrected source image, independent of its pixel size. */
data class CropRect(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
}

/** Size of the image that should be produced from a crop. */
data class CropOutputSize(val width: Int, val height: Int)

/**
 * Immutable snapshot for rendering. Coordinates are in the container's pixel/point space.
 *
 * Draw the image with its centre at ([imageCenterX], [imageCenterY]) scaled by [scale]
 * (container units per image pixel) and clip/dim everything outside the crop window.
 */
data class CropState(
    val containerWidth: Float = 0f,
    val containerHeight: Float = 0f,
    val windowLeft: Float = 0f,
    val windowTop: Float = 0f,
    val windowWidth: Float = 0f,
    val windowHeight: Float = 0f,
    val scale: Float = 1f,
    val imageCenterX: Float = 0f,
    val imageCenterY: Float = 0f
) {
    val isLaidOut: Boolean get() = windowWidth > 0f && windowHeight > 0f
}

/**
 * Platform-independent crop logic with a fixed window aspect ratio. Reusable for any image crop:
 * the UI only renders [CropState], forwards gestures to [transform] and renders the final bitmap
 * from [cropRect] / [outputSize].
 *
 * @param imageWidth  width in pixels of the orientation-corrected source image
 * @param imageHeight height in pixels of the orientation-corrected source image
 * @param aspectRatio crop window width / height (1 = square, 16/9 = landscape, ...)
 * @param maxZoom     maximum zoom relative to the minimum (image just covers the window)
 * @param windowInset margin between container edge and crop window
 */
class ImageCropper(
    private val imageWidth: Int,
    private val imageHeight: Int,
    private val aspectRatio: Float = 1f,
    private val maxZoom: Float = 8f,
    private val windowInset: Float = 24f
) {
    init {
        require(imageWidth > 0 && imageHeight > 0) { "Image size must be positive" }
        require(aspectRatio > 0f) { "Aspect ratio must be positive" }
    }

    var state: CropState = CropState()
        private set

    /** (Re)computes the crop window for a container size, preserving the current crop when already laid out. */
    fun layout(containerWidth: Float, containerHeight: Float): CropState {
        if (containerWidth <= 0f || containerHeight <= 0f) return state
        val previous = if (state.isLaidOut) cropRect() else null

        val availableW = max(containerWidth - 2 * windowInset, 1f)
        val availableH = max(containerHeight - 2 * windowInset, 1f)
        val windowW: Float
        val windowH: Float
        if (availableW / availableH > aspectRatio) {
            windowH = availableH; windowW = availableH * aspectRatio
        } else {
            windowW = availableW; windowH = availableW / aspectRatio
        }
        val windowLeft = (containerWidth - windowW) / 2f
        val windowTop = (containerHeight - windowH) / 2f

        val minScale = minScale(windowW, windowH)
        val scale: Float
        val centerOffsetX: Float
        val centerOffsetY: Float
        if (previous != null) {
            // Keep showing the same part of the image after a container size change.
            scale = (windowW / (previous.width * imageWidth)).coerceIn(minScale, minScale * maxZoom)
            centerOffsetX = (0.5f - (previous.left + previous.right) / 2f) * imageWidth * scale
            centerOffsetY = (0.5f - (previous.top + previous.bottom) / 2f) * imageHeight * scale
        } else {
            scale = minScale; centerOffsetX = 0f; centerOffsetY = 0f
        }
        state = clamp(
            CropState(
                containerWidth, containerHeight, windowLeft, windowTop, windowW, windowH, scale,
                windowLeft + windowW / 2f + centerOffsetX, windowTop + windowH / 2f + centerOffsetY
            )
        )
        return state
    }

    /**
     * Applies one gesture step: translate by ([panX], [panY]) and multiply the zoom by [zoom] around
     * the container point ([centroidX], [centroidY]). The image always keeps covering the window.
     */
    fun transform(panX: Float, panY: Float, zoom: Float, centroidX: Float, centroidY: Float): CropState {
        val s = state
        if (!s.isLaidOut) return s
        val minScale = minScale(s.windowWidth, s.windowHeight)
        val newScale = (s.scale * zoom).coerceIn(minScale, minScale * maxZoom)
        val factor = newScale / s.scale
        // Keep the image point under the centroid fixed, then add the pan.
        val cx = centroidX - (centroidX - s.imageCenterX) * factor + panX
        val cy = centroidY - (centroidY - s.imageCenterY) * factor + panY
        state = clamp(s.copy(scale = newScale, imageCenterX = cx, imageCenterY = cy))
        return state
    }

    /** Resets to the initial, centred, fully covering position. */
    fun reset(): CropState {
        val s = state
        if (!s.isLaidOut) return s
        state = s.copy(
            scale = minScale(s.windowWidth, s.windowHeight),
            imageCenterX = s.windowLeft + s.windowWidth / 2f,
            imageCenterY = s.windowTop + s.windowHeight / 2f
        )
        return state
    }

    /** The visible window expressed as fractions of the source image. */
    fun cropRect(): CropRect {
        val s = state
        if (!s.isLaidOut) return CropRect(0f, 0f, 1f, 1f)
        val left = (s.windowLeft - s.imageCenterX) / s.scale + imageWidth / 2f
        val top = (s.windowTop - s.imageCenterY) / s.scale + imageHeight / 2f
        val right = left + s.windowWidth / s.scale
        val bottom = top + s.windowHeight / s.scale
        return CropRect(
            (left / imageWidth).coerceIn(0f, 1f),
            (top / imageHeight).coerceIn(0f, 1f),
            (right / imageWidth).coerceIn(0f, 1f),
            (bottom / imageHeight).coerceIn(0f, 1f)
        )
    }

    /**
     * Output dimensions: the crop scaled so its longer edge is at most [maxEdge] pixels.
     * Crops smaller than [maxEdge] are not upscaled.
     */
    fun outputSize(maxEdge: Int): CropOutputSize {
        val rect = cropRect()
        val cropW = rect.width * imageWidth
        val cropH = rect.height * imageHeight
        val factor = min(1f, maxEdge / max(cropW, cropH))
        return CropOutputSize(
            width = max(1, (cropW * factor).roundToInt()),
            height = max(1, (cropH * factor).roundToInt())
        )
    }

    private fun minScale(windowW: Float, windowH: Float): Float =
        max(windowW / imageWidth, windowH / imageHeight)

    private fun clamp(s: CropState): CropState {
        val maxDx = max(0f, (imageWidth * s.scale - s.windowWidth) / 2f)
        val maxDy = max(0f, (imageHeight * s.scale - s.windowHeight) / 2f)
        val windowCx = s.windowLeft + s.windowWidth / 2f
        val windowCy = s.windowTop + s.windowHeight / 2f
        return s.copy(
            imageCenterX = s.imageCenterX.coerceIn(windowCx - maxDx, windowCx + maxDx),
            imageCenterY = s.imageCenterY.coerceIn(windowCy - maxDy, windowCy + maxDy)
        )
    }
}
