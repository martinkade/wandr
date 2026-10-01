package com.wandr.presentation.imagecrop

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ImageCropperTest {

    private fun assertNear(expected: Float, actual: Float, eps: Float = 0.001f) =
        assertTrue(abs(expected - actual) <= eps, "expected $expected but was $actual")

    private fun cropper(w: Int, h: Int, ratio: Float = 1f) =
        ImageCropper(w, h, ratio, windowInset = 0f).also { it.layout(400f, 800f) }

    @Test
    fun squareWindowFitsContainerWidth() {
        val s = cropper(2000, 1000).state
        assertEquals(400f, s.windowWidth)
        assertEquals(400f, s.windowHeight)
        assertEquals(200f, s.windowTop)
    }

    @Test
    fun landscapeImageInitiallyShowsCentredSquare() {
        val r = cropper(2000, 1000).cropRect()
        assertNear(0.25f, r.left); assertNear(0.75f, r.right)
        assertNear(0f, r.top); assertNear(1f, r.bottom)
    }

    @Test
    fun customAspectRatioIsRespected() {
        val s = cropper(2000, 1000, ratio = 2f).state
        assertNear(2f, s.windowWidth / s.windowHeight)
    }

    @Test
    fun panIsClampedToImageBounds() {
        val c = cropper(2000, 1000)
        c.transform(panX = 100_000f, panY = 100_000f, zoom = 1f, centroidX = 0f, centroidY = 0f)
        val r = c.cropRect()
        assertNear(0f, r.left); assertNear(0.5f, r.right)
        assertNear(0f, r.top); assertNear(1f, r.bottom)
    }

    @Test
    fun zoomOutIsClampedSoImageStillCoversWindow() {
        val c = cropper(2000, 1000)
        c.transform(0f, 0f, zoom = 0.01f, centroidX = 200f, centroidY = 400f)
        val r = c.cropRect()
        assertNear(0.5f, r.width); assertNear(1f, r.height)
    }

    @Test
    fun zoomInHalvesVisibleArea() {
        val c = cropper(2000, 1000)
        c.transform(0f, 0f, zoom = 2f, centroidX = 200f, centroidY = 400f)
        val r = c.cropRect()
        assertNear(0.25f, r.width); assertNear(0.5f, r.height)
        assertNear(0.5f, (r.left + r.right) / 2f)
    }

    @Test
    fun zoomKeepsPointUnderCentroidFixed() {
        val c = cropper(2000, 2000)
        // Zoom around the top-left corner of the window: its image coordinate must not move.
        val before = c.cropRect()
        c.transform(0f, 0f, zoom = 2f, centroidX = 0f, centroidY = 200f)
        val after = c.cropRect()
        assertNear(before.left, after.left)
        assertNear(before.top, after.top)
    }

    @Test
    fun outputIsScaledDownToMaxEdge() {
        val out = cropper(4000, 3000).outputSize(512)
        assertEquals(CropOutputSize(512, 512), out)
    }

    @Test
    fun smallCropsAreNotUpscaled() {
        val out = cropper(300, 300).outputSize(512)
        assertEquals(CropOutputSize(300, 300), out)
    }

    @Test
    fun relayoutPreservesCrop() {
        val c = cropper(2000, 1000)
        c.transform(-40f, 0f, 2f, 200f, 400f)
        val before = c.cropRect()
        c.layout(600f, 600f)
        val after = c.cropRect()
        assertNear(before.left, after.left, 0.01f)
        assertNear(before.top, after.top, 0.01f)
        assertNear(before.width, after.width, 0.01f)
    }

    @Test
    fun coverSpecProduces4to3OutputAtMost1024Wide() {
        val cropper = ImageCropper(4000, 3000, CoverImageSpec.ASPECT_RATIO, windowInset = 0f).also { it.layout(400f, 800f) }
        val out = cropper.outputSize(CoverImageSpec.MAX_EDGE_PX)
        assertEquals(CropOutputSize(1024, 768), out)
    }

    @Test
    fun coverSpecDoesNotUpscaleSmallImages() {
        val cropper = ImageCropper(800, 600, CoverImageSpec.ASPECT_RATIO, windowInset = 0f).also { it.layout(400f, 800f) }
        assertEquals(CropOutputSize(800, 600), cropper.outputSize(CoverImageSpec.MAX_EDGE_PX))
    }
}
