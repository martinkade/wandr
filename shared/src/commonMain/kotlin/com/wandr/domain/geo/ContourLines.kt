package com.wandr.domain.geo

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Elevation lines (contours) like on a topographic map, generated from a smooth terrain: a few hills and valleys
 * plus a gentle ripple, cut at evenly spaced heights (marching squares). Used as the decorative background of the app's
 * design language. The result only depends on the arguments, so Android and iOS draw the same lines.
 */
object ContourLines {

    /**
     * @property segments `x1, y1, x2, y2` per segment, all in 0..1 of the drawing area (multiply by its size)
     * @property levels the height level of each segment (0 until [levelCount]); neighbouring levels are neighbouring lines
     */
    class ContourSet(val segments: FloatArray, val levels: IntArray, val levelCount: Int) {
        val segmentCount: Int get() = levels.size
    }

    /**
     * @param aspect width / height of the area, so the terrain is not stretched
     * @param columns grid resolution along the width; more means smoother lines and more segments
     * @param levels number of height levels (lines)
     */
    fun generate(aspect: Float, seed: Int = 7, columns: Int = 44, levels: Int = 18): ContourSet {
        val ratio = aspect.coerceIn(0.2f, 5f).toDouble()
        val rows = (columns / ratio).toInt().coerceIn(8, 400)
        val hills = hills(seed, ratio)

        // Field values on the grid points; x spans 0..ratio, y spans 0..1.
        val field = Array(rows + 1) { r ->
            DoubleArray(columns + 1) { c ->
                height(
                    hills,
                    c.toDouble() / columns * ratio,
                    r.toDouble() / rows
                )
            }
        }
        val min = field.minOf { row -> row.min() }
        val max = field.maxOf { row -> row.max() }

        val coords = ArrayList<Float>()
        val levelOf = ArrayList<Int>()
        for (level in 0 until levels) {
            val threshold = min + (level + 1).toDouble() / (levels + 1) * (max - min)
            for (r in 0 until rows) {
                for (c in 0 until columns) {
                    cell(field, r, c, threshold) { x1, y1, x2, y2 ->
                        // grid coordinates -> 0..1
                        coords += (x1 / columns).toFloat(); coords += (y1 / rows).toFloat()
                        coords += (x2 / columns).toFloat(); coords += (y2 / rows).toFloat()
                        levelOf += level
                    }
                }
            }
        }
        return ContourSet(coords.toFloatArray(), levelOf.toIntArray(), levels)
    }

    private class Hill(val x: Double, val y: Double, val radius: Double, val amplitude: Double)

    /** A small deterministic pseudo random generator, so the same seed gives the same terrain on every platform. */
    private fun hills(seed: Int, ratio: Double): List<Hill> {
        var state = seed.toLong() * 2862933555777941757L + 3037000493L
        fun next(): Double {
            state = state * 6364136223846793005L + 1442695040888963407L
            return ((state ushr 33) and 0x7fffffffL).toDouble() / 0x7fffffffL.toDouble()
        }
        return List(HILLS) {
            Hill(
                x = -0.1 * ratio + next() * 1.2 * ratio,
                y = -0.1 + next() * 1.2,
                radius = 0.12 + next() * 0.22,
                amplitude = if (next() < 0.5) 1.0 else -0.8 * (0.5 + next())
            )
        }
    }

    private fun height(hills: List<Hill>, x: Double, y: Double): Double {
        var h = 0.0
        for (hill in hills) {
            val dx = x - hill.x
            val dy = y - hill.y
            h += hill.amplitude * exp(-(dx * dx + dy * dy) / (2 * hill.radius * hill.radius))
        }
        // A gentle ripple makes the lines wavy instead of perfect ovals.
        return h + 0.12 * sin(3.1 * x + 1.7 * sin(2.3 * y)) + 0.08 * sin(5.3 * y + 2.1 * sin(1.9 * x) + PI / 3)
    }

    /** Where between two corner values the line at [threshold] crosses their edge, 0..1 from [a] to [b]. */
    private fun crossing(a: Double, b: Double, threshold: Double): Double =
        if (b == a) 0.5 else ((threshold - a) / (b - a)).coerceIn(0.0, 1.0)

    /** The line segments of one grid cell at [threshold]; coordinates are in grid units. */
    private inline fun cell(
        field: Array<DoubleArray>,
        r: Int,
        c: Int,
        threshold: Double,
        emit: (Double, Double, Double, Double) -> Unit
    ) {
        val tl = field[r][c]
        val tr = field[r][c + 1]
        val br = field[r + 1][c + 1]
        val bl = field[r + 1][c]
        var index = 0
        if (tl > threshold) index = index or 8
        if (tr > threshold) index = index or 4
        if (br > threshold) index = index or 2
        if (bl > threshold) index = index or 1
        if (index == 0 || index == 15) return

        val x0 = c.toDouble()
        val y0 = r.toDouble()
        // Crossing points on the four edges
        val topX = x0 + crossing(tl, tr, threshold);
        val topY = y0
        val rightX = x0 + 1;
        val rightY = y0 + crossing(tr, br, threshold)
        val bottomX = x0 + crossing(bl, br, threshold);
        val bottomY = y0 + 1
        val leftX = x0;
        val leftY = y0 + crossing(tl, bl, threshold)

        when (index) {
            1, 14 -> emit(leftX, leftY, bottomX, bottomY)
            2, 13 -> emit(bottomX, bottomY, rightX, rightY)
            3, 12 -> emit(leftX, leftY, rightX, rightY)
            4, 11 -> emit(topX, topY, rightX, rightY)
            6, 9 -> emit(topX, topY, bottomX, bottomY)
            7, 8 -> emit(leftX, leftY, topX, topY)
            5, 10 -> {
                // Saddle: the centre decides how the two lines connect.
                val centre = (tl + tr + br + bl) / 4
                val connectTopLeft = (centre > threshold) == (index == 10)
                if (connectTopLeft) {
                    emit(leftX, leftY, topX, topY); emit(bottomX, bottomY, rightX, rightY)
                } else {
                    emit(leftX, leftY, bottomX, bottomY); emit(topX, topY, rightX, rightY)
                }
            }
        }
    }

    private const val HILLS = 9
}
