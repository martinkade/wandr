package com.wandr.presentation.imagecrop

/** Output spec for profile avatars; shared so Android and iOS produce identical uploads. */
object AvatarImageSpec {
    const val ASPECT_RATIO = 1f
    const val MAX_EDGE_PX = 512
    const val JPEG_QUALITY = 85 // percent
}
