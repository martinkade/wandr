package com.wandr.data.fit

import com.wandr.domain.model.GpsTrackpoint

/**
 * Decodes Garmin FIT binary files back into GPS trackpoints and activity metrics.
 */
object FitFileDecoder {

    fun decodeTrackpoints(fitBytes: ByteArray): List<GpsTrackpoint> {
        if (fitBytes.size < 16) return emptyList()
        val headerSize = fitBytes[0].toInt() and 0xFF
        if (headerSize < 12) return emptyList()
        
        // Verify magic bytes ".FIT"
        val magic = fitBytes.decodeToString(8, 12)
        if (magic != ".FIT") return emptyList()

        val trackpoints = mutableListOf<GpsTrackpoint>()
        var offset = headerSize
        val endOffset = fitBytes.size - 2
        
        val recordSizes = mutableMapOf<Int, Int>() // localMsgId -> byte size

        while (offset < endOffset) {
            val headerByte = fitBytes[offset].toInt() and 0xFF
            val isDefinition = (headerByte and 0x40) != 0
            val localMsgId = headerByte and 0x0F

            if (isDefinition) {
                // FIT Definition Message
                if (offset + 6 <= endOffset) {
                    val numFields = fitBytes[offset + 5].toInt() and 0xFF
                    var recordSize = 1 // 1 byte for header
                    val fieldStart = offset + 6
                    for (f in 0 until numFields) {
                        val fIndex = fieldStart + (f * 3)
                        if (fIndex + 1 < endOffset) {
                            val fSize = fitBytes[fIndex + 1].toInt() and 0xFF
                            recordSize += fSize
                        }
                    }
                    recordSizes[localMsgId] = recordSize
                    offset += 6 + (numFields * 3)
                } else {
                    break
                }
            } else {
                // FIT Data Message
                val recordSize = recordSizes[localMsgId] ?: 17
                if (localMsgId == 1 && offset + 17 <= endOffset) {
                    // Local Msg ID 1 is our Trackpoint record
                    val latSemicircles = readInt32LE(fitBytes, offset + 1)
                    val lonSemicircles = readInt32LE(fitBytes, offset + 5)
                    val altScaled = readUInt16LE(fitBytes, offset + 9)
                    val speedScaled = readUInt16LE(fitBytes, offset + 11)
                    val timestampSec = readInt32LE(fitBytes, offset + 13)

                    val lat = latSemicircles * (180.0 / 2147483648.0)
                    val lon = lonSemicircles * (180.0 / 2147483648.0)
                    val alt = (altScaled.toDouble() / 5.0) - 500.0
                    val speed = speedScaled.toFloat() / 1000.0f
                    val timestampMs = timestampSec.toLong() * 1000L

                    if (lat >= -90.0 && lat <= 90.0 && lon >= -180.0 && lon <= 180.0) {
                        trackpoints.add(
                            GpsTrackpoint(
                                latitude = lat,
                                longitude = lon,
                                altitudeMeters = alt,
                                timestamp = timestampMs,
                                speedMetersPerSecond = speed
                            )
                        )
                    }
                }
                offset += recordSize
            }
        }

        return trackpoints
    }

    private fun readInt32LE(bytes: ByteArray, offset: Int): Int {
        return (bytes[offset].toInt() and 0xFF) or
                ((bytes[offset + 1].toInt() and 0xFF) shl 8) or
                ((bytes[offset + 2].toInt() and 0xFF) shl 16) or
                ((bytes[offset + 3].toInt() and 0xFF) shl 24)
    }

    private fun readUInt16LE(bytes: ByteArray, offset: Int): Int {
        return (bytes[offset].toInt() and 0xFF) or
                ((bytes[offset + 1].toInt() and 0xFF) shl 8)
    }
}
