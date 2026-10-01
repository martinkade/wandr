package com.wandr.presentation.imagecrop

/**
 * Output spec for team (and challenge) cover images: 4:3 landscape, at most 1024 px wide.
 * The crop is landscape, so its longer edge is its width and `MAX_EDGE_PX` is the maximum width.
 */
object CoverImageSpec {
    const val ASPECT_RATIO = 4f / 3f
    const val MAX_EDGE_PX = 1024
    const val JPEG_QUALITY = 85 // percent
}
