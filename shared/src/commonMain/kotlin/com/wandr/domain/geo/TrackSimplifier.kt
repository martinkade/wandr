package com.wandr.domain.geo

import com.wandr.domain.model.GpsTrackpoint
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot

/**
 * Reduces a track to the points that matter for drawing its route (Ramer-Douglas-Peucker), so the stored polyline of
 * a long activity stays small. Distances are approximated on a flat plane, which is exact enough at this scale.
 */
object TrackSimplifier {
    private const val METERS_PER_DEGREE = 111_320.0

    /**
     * @param toleranceMeters how far the route may deviate from the original
     * @param maxPoints upper bound; the tolerance is raised until the result fits
     */
    fun simplify(points: List<GpsTrackpoint>, toleranceMeters: Double = 6.0, maxPoints: Int = 500): List<GpsTrackpoint> {
        if (points.size <= 2) return points
        var tolerance = toleranceMeters
        var result = rdp(points, tolerance)
        while (result.size > maxPoints) {
            tolerance *= 1.6
            result = rdp(points, tolerance)
        }
        return result
    }

    private fun rdp(points: List<GpsTrackpoint>, tolerance: Double): List<GpsTrackpoint> {
        val keep = BooleanArray(points.size)
        keep[0] = true
        keep[points.lastIndex] = true
        val stack = ArrayDeque<Pair<Int, Int>>()
        stack.addLast(0 to points.lastIndex)
        val lonScale = cos(points.first().latitude * PI / 180.0)
        while (stack.isNotEmpty()) {
            val (first, last) = stack.removeLast()
            var maxDistance = 0.0
            var index = -1
            for (i in first + 1 until last) {
                val d = distanceToSegment(points[i], points[first], points[last], lonScale)
                if (d > maxDistance) {
                    maxDistance = d
                    index = i
                }
            }
            if (index != -1 && maxDistance > tolerance) {
                keep[index] = true
                stack.addLast(first to index)
                stack.addLast(index to last)
            }
        }
        return points.filterIndexed { i, _ -> keep[i] }
    }

    private fun distanceToSegment(p: GpsTrackpoint, a: GpsTrackpoint, b: GpsTrackpoint, lonScale: Double): Double {
        val ax = a.longitude * lonScale * METERS_PER_DEGREE
        val ay = a.latitude * METERS_PER_DEGREE
        val bx = b.longitude * lonScale * METERS_PER_DEGREE
        val by = b.latitude * METERS_PER_DEGREE
        val px = p.longitude * lonScale * METERS_PER_DEGREE
        val py = p.latitude * METERS_PER_DEGREE
        val dx = bx - ax
        val dy = by - ay
        val lengthSquared = dx * dx + dy * dy
        if (lengthSquared == 0.0) return hypot(px - ax, py - ay)
        val t = (((px - ax) * dx + (py - ay) * dy) / lengthSquared).coerceIn(0.0, 1.0)
        return hypot(px - (ax + t * dx), py - (ay + t * dy))
    }
}
