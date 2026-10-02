package com.wandr.domain.watch

import okio.ByteString.Companion.encodeUtf8

/**
 * Stable activity id for a workout read from a health platform. The ids of those platforms are not UUIDs (the server
 * needs UUIDs), but the same workout must always get the same id so that reading it again imports it only once.
 */
object ExternalWorkoutId {
    /** A name-based (MD5, UUID version 3 layout) UUID of [source] + [externalId]. */
    fun from(source: String, externalId: String): String {
        val bytes = "$source:$externalId".encodeUtf8().md5().toByteArray()
        bytes[6] = ((bytes[6].toInt() and 0x0F) or 0x30).toByte()
        bytes[8] = ((bytes[8].toInt() and 0x3F) or 0x80).toByte()
        val hex = bytes.joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }
        return "${hex.substring(0, 8)}-${hex.substring(8, 12)}-${hex.substring(12, 16)}-${hex.substring(16, 20)}-${hex.substring(20)}"
    }
}
