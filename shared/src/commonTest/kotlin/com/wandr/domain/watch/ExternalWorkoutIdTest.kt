package com.wandr.domain.watch

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ExternalWorkoutIdTest {
    @Test
    fun isStableAndUuidShaped() {
        val id = ExternalWorkoutId.from(WorkoutSource.HEALTH_CONNECT, "record-1")
        assertEquals(id, ExternalWorkoutId.from(WorkoutSource.HEALTH_CONNECT, "record-1"))
        assertTrue(Regex("[0-9a-f]{8}-[0-9a-f]{4}-3[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}").matches(id), id)
    }

    @Test
    fun differsPerRecordAndSource() {
        val a = ExternalWorkoutId.from(WorkoutSource.HEALTH_CONNECT, "record-1")
        assertNotEquals(a, ExternalWorkoutId.from(WorkoutSource.HEALTH_CONNECT, "record-2"))
        assertNotEquals(a, ExternalWorkoutId.from(WorkoutSource.APPLE_HEALTH, "record-1"))
    }

    @Test
    fun sourceSurvivesTheWireFormat() {
        val w = WatchWorkout("x", "hiking", 1, 2, 3.0, source = WorkoutSource.APPLE_HEALTH)
        assertEquals(WorkoutSource.APPLE_HEALTH, WatchWorkoutCodec.decode(WatchWorkoutCodec.encode(w))?.source)
        assertEquals(WorkoutSource.WATCH, WatchWorkoutCodec.decode("""{"id":"a","activity_type":"x","start_time":1,"end_time":2,"distance_meters":0}""")?.source)
    }
}
