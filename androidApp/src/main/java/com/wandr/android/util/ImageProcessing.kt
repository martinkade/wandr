package com.wandr.android.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import com.wandr.presentation.imagecrop.CropOutputSize
import com.wandr.presentation.imagecrop.CropRect
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Decoding, EXIF orientation handling, cropping and scaling for picked/captured images. */
object ImageProcessing {

    data class Size(val width: Int, val height: Int)

    /** Pixel size of the image after applying its EXIF orientation. */
    fun orientedSize(bytes: ByteArray): Size {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Unsupported image" }
        return if (exifOrientation(bytes).swapsAxes()) Size(bounds.outHeight, bounds.outWidth)
        else Size(bounds.outWidth, bounds.outHeight)
    }

    /** Decodes an oriented bitmap whose longer edge is at most [maxEdge] pixels (for on-screen display). */
    fun decodeForDisplay(bytes: ByteArray, maxEdge: Int = 2048): Bitmap {
        val size = orientedSize(bytes)
        return decodeOriented(bytes, sampleSizeFor(max(size.width, size.height).toFloat() / maxEdge))
    }

    /**
     * Crops [rect] (fractions of the oriented image) out of the original and scales it to [output].
     * The source is decoded only as large as needed, then encoded as JPEG.
     */
    fun cropAndScale(bytes: ByteArray, rect: CropRect, output: CropOutputSize, quality: Int): ByteArray {
        val full = orientedSize(bytes)
        val cropW = rect.width * full.width
        val cropH = rect.height * full.height
        val neededScale = min(1f, max(output.width / cropW, output.height / cropH))
        val decoded = decodeOriented(bytes, sampleSizeFor(1f / neededScale))

        val x = (rect.left * decoded.width).roundToInt().coerceIn(0, decoded.width - 1)
        val y = (rect.top * decoded.height).roundToInt().coerceIn(0, decoded.height - 1)
        val w = (rect.width * decoded.width).roundToInt().coerceIn(1, decoded.width - x)
        val h = (rect.height * decoded.height).roundToInt().coerceIn(1, decoded.height - y)

        val cropped = Bitmap.createBitmap(decoded, x, y, w, h)
        val scaled = if (cropped.width == output.width && cropped.height == output.height) cropped
        else Bitmap.createScaledBitmap(cropped, output.width, output.height, true)

        return ByteArrayOutputStream().use { out ->
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
            out.toByteArray()
        }.also {
            if (scaled !== cropped) scaled.recycle()
            if (cropped !== decoded) cropped.recycle()
            decoded.recycle()
        }
    }

    /** Largest power-of-two sample size not exceeding [ratio] (>= 1). */
    private fun sampleSizeFor(ratio: Float): Int {
        var sample = 1
        while (sample * 2 <= ceil(ratio).toInt()) sample *= 2
        return sample
    }

    private fun decodeOriented(bytes: ByteArray, sampleSize: Int): Bitmap {
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            ?: throw IllegalArgumentException("Unsupported image")
        val matrix = orientationMatrix(exifOrientation(bytes)) ?: return bitmap
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            .also { if (it !== bitmap) bitmap.recycle() }
    }

    private fun exifOrientation(bytes: ByteArray): Int = runCatching {
        ExifInterface(ByteArrayInputStream(bytes))
            .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

    private fun Int.swapsAxes() = this == ExifInterface.ORIENTATION_ROTATE_90 ||
        this == ExifInterface.ORIENTATION_ROTATE_270 ||
        this == ExifInterface.ORIENTATION_TRANSPOSE ||
        this == ExifInterface.ORIENTATION_TRANSVERSE

    private fun orientationMatrix(orientation: Int): Matrix? {
        val m = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> m.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> m.postRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> m.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> { m.postRotate(90f); m.postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_ROTATE_90 -> m.postRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> { m.postRotate(-90f); m.postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_ROTATE_270 -> m.postRotate(270f)
            else -> return null
        }
        return m
    }
}
