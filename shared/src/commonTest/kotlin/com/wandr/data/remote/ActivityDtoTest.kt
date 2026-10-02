package com.wandr.data.remote

import com.wandr.data.local.entity.ActivityEntity
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ActivityDtoTest {

    private val entity = ActivityEntity(
        id = "11111111-1111-1111-1111-111111111111",
        userId = "u1",
        teamId = null,
        title = "Trail walk",
        description = null,
        activityType = "hiking",
        distanceMeters = 5400.0,
        durationSeconds = 3600.0,
        elevationGainMeters = 150.0,
        fitFilePath = "/data/user/0/app/files/fit/11111111-1111-1111-1111-111111111111.fit",
        startTime = 0L,
        endTime = 3_600_000L,
        isManualEntry = false,
        createdAt = 0L,
        updatedAt = 0L
    )

    @Test
    fun localFitFilePathIsNeverPartOfTheUploadPayload() {
        val json = Json { encodeDefaults = true }.encodeToString(entity.toDto())

        assertFalse(json.contains("fit"), "FIT path must stay on the device: $json")
        assertFalse(json.contains("/data/user"), json)
    }

    @Test
    fun usesSnakeCaseColumnsAndIsoTimestamps() {
        val dto = entity.toDto()
        val json = Json.encodeToString(dto)

        assertTrue(json.contains("\"activity_type\":\"hiking\""))
        assertTrue(json.contains("\"distance_meters\":5400.0"))
        assertEquals("1970-01-01T01:00:00Z", dto.endTime)
    }
}
