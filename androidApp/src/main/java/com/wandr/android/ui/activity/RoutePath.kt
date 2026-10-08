package com.wandr.android.ui.activity

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path

/**
 * The route as a smooth line instead of a polygon: it runs through the middle of each segment and uses the track points as
 * the control points of the curves in between. That rounds the small kinks that GPS jitter leaves, without leaving the
 * track (the line stays inside the polygon of the points).
 */
internal fun smoothedPath(points: List<Offset>): Path {
    val path = Path()
    if (points.isEmpty()) return path
    path.moveTo(points[0].x, points[0].y)
    when {
        points.size == 2 -> path.lineTo(points[1].x, points[1].y)
        points.size > 2 -> {
            for (i in 1 until points.lastIndex) {
                val mid = Offset((points[i].x + points[i + 1].x) / 2, (points[i].y + points[i + 1].y) / 2)
                path.quadraticTo(points[i].x, points[i].y, mid.x, mid.y)
            }
            path.lineTo(points.last().x, points.last().y)
        }
    }
    return path
}
