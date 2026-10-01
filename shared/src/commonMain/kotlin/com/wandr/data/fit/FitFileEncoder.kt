package com.wandr.data.fit

import com.wandr.domain.model.GpsTrackpoint

/**
 * Encodes GPS trackpoints and activity metrics into a standard Garmin FIT binary byte array format.
 */
object FitFileEncoder {
    
    fun encode(
        activityType: String,
        startTimeMs: Long,
        endTimeMs: Long,
        distanceMeters: Double,
        elevationGainMeters: Double,
        trackpoints: List<GpsTrackpoint>
    ): ByteArray {
        val payloadBytes = mutableListOf<Byte>()
        
        // 1. File Id definition and record (Local Msg 0)
        val fileIdDef = byteArrayOf(
            0x40, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x01, 0x00
        )
        payloadBytes.addAll(fileIdDef.toList())
        
        val fileIdRecord = byteArrayOf(
            0x00, 0x04 // activity type = 4 (fitness)
        )
        payloadBytes.addAll(fileIdRecord.toList())

        // 2. Trackpoint Record Definition (Local Msg 1)
        // Fields:
        // Field 0: position_lat (sint32, 4 bytes, semicircles)
        // Field 1: position_long (sint32, 4 bytes, semicircles)
        // Field 2: altitude (uint16, 2 bytes, 5 * meters + 500)
        // Field 3: speed (uint16, 2 bytes, 1000 * m/s)
        // Field 253: timestamp (uint32, 4 bytes)
        val trackpointDef = byteArrayOf(
            0x41, 0x00, 0x00, 0x00, 0x14, 0x05,
            0x00, 0x04, 0x83.toByte(), // position_lat (sint32)
            0x01, 0x04, 0x83.toByte(), // position_long (sint32)
            0x02, 0x02, 0x84.toByte(), // altitude (uint16)
            0x03, 0x02, 0x84.toByte(), // speed (uint16)
            0xFD.toByte(), 0x04, 0x8C.toByte() // timestamp (uint32)
        )
        payloadBytes.addAll(trackpointDef.toList())

        // 3. Trackpoint data records (Local Msg 1 header = 0x01)
        for (tp in trackpoints) {
            val recordHeader = 0x01.toByte()
            val latSemicircles = (tp.latitude * (2147483648.0 / 180.0)).toInt()
            val lonSemicircles = (tp.longitude * (2147483648.0 / 180.0)).toInt()
            val altScaled = ((tp.altitudeMeters + 500.0) * 5.0).toInt().coerceIn(0, 65535)
            val speedScaled = (tp.speedMetersPerSecond * 1000.0).toInt().coerceIn(0, 65535)
            val timestampSec = (tp.timestamp / 1000).toInt()

            val recordData = ByteArray(17)
            recordData[0] = recordHeader
            
            // Lat (Little Endian)
            recordData[1] = latSemicircles.toByte()
            recordData[2] = (latSemicircles shr 8).toByte()
            recordData[3] = (latSemicircles shr 16).toByte()
            recordData[4] = (latSemicircles shr 24).toByte()
            
            // Lon (Little Endian)
            recordData[5] = lonSemicircles.toByte()
            recordData[6] = (lonSemicircles shr 8).toByte()
            recordData[7] = (lonSemicircles shr 16).toByte()
            recordData[8] = (lonSemicircles shr 24).toByte()
            
            // Alt (Little Endian)
            recordData[9] = altScaled.toByte()
            recordData[10] = (altScaled shr 8).toByte()
            
            // Speed (Little Endian)
            recordData[11] = speedScaled.toByte()
            recordData[12] = (speedScaled shr 8).toByte()
            
            // Timestamp (Little Endian)
            recordData[13] = timestampSec.toByte()
            recordData[14] = (timestampSec shr 8).toByte()
            recordData[15] = (timestampSec shr 16).toByte()
            recordData[16] = (timestampSec shr 24).toByte()

            payloadBytes.addAll(recordData.toList())
        }

        val dataSize = payloadBytes.size
        val header = ByteArray(14)
        header[0] = 14 // Header size
        header[1] = 0x20 // Protocol version 2.0
        header[2] = 0x34 // Profile version low
        header[3] = 0x08 // Profile version high
        
        // Data Size (4 bytes, Little Endian)
        header[4] = dataSize.toByte()
        header[5] = (dataSize shr 8).toByte()
        header[6] = (dataSize shr 16).toByte()
        header[7] = (dataSize shr 24).toByte()

        // Magic bytes ".FIT"
        header[8] = '.'.code.toByte()
        header[9] = 'F'.code.toByte()
        header[10] = 'I'.code.toByte()
        header[11] = 'T'.code.toByte()

        // Header CRC (2 bytes)
        val headerCrc = computeCrc16(header, 0, 12)
        header[12] = headerCrc.toByte()
        header[13] = (headerCrc shr 8).toByte()

        val fullFile = mutableListOf<Byte>()
        fullFile.addAll(header.toList())
        fullFile.addAll(payloadBytes)

        // File CRC (2 bytes)
        val fileCrc = computeCrc16(fullFile.toByteArray(), 0, fullFile.size)
        fullFile.add(fileCrc.toByte())
        fullFile.add((fileCrc shr 8).toByte())

        return fullFile.toByteArray()
    }

    private fun computeCrc16(data: ByteArray, start: Int, length: Int): Int {
        val crcTable = intArrayOf(
            0x0000, 0xCC01, 0xD801, 0x1400, 0xF001, 0x3C00, 0x2800, 0xE401,
            0xA001, 0x6C00, 0x7800, 0xB401, 0x5000, 0x9C01, 0x8801, 0x4400
        )
        var crc = 0
        for (i in start until (start + length)) {
            val byteVal = data[i].toInt() and 0xFF
            var tmp = crcTable[crc and 0x0F]
            crc = (crc shr 4) and 0x0FFF
            crc = crc xor tmp xor crcTable[byteVal and 0x0F]

            tmp = crcTable[crc and 0x0F]
            crc = (crc shr 4) and 0x0FFF
            crc = crc xor tmp xor crcTable[(byteVal shr 4) and 0x0F]
        }
        return crc
    }
}
