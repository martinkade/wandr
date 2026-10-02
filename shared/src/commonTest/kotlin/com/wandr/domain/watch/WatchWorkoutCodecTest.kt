package com.wandr.domain.watch

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WatchWorkoutCodecTest {
    private val workout = WatchWorkout(
        id = "w1", activityType = "running", startTime = 1_000L, endTime = 61_000L, distanceMeters = 250.0,
        elevationGainMeters = 4.0, averageHeartRate = 140, maxHeartRate = 171,
        trackpoints = listOf(WatchTrackpoint(47.1, 8.2, 400.0, 1_000L, 2.5f))
    )

    @Test
    fun roundTrips() {
        assertEquals(workout, WatchWorkoutCodec.decode(WatchWorkoutCodec.encode(workout)))
    }

    @Test
    fun usesTheAgreedSnakeCaseKeys() {
        val json = WatchWorkoutCodec.encode(workout)
        listOf("\"activity_type\"", "\"start_time\"", "\"end_time\"", "\"distance_meters\"", "\"elevation_gain_meters\"",
            "\"average_heart_rate\"", "\"max_heart_rate\"", "\"lat\"", "\"lon\"", "\"alt\"", "\"t\"", "\"version\"")
            .forEach { assertTrue(json.contains(it), "missing $it in $json") }
    }

    @Test
    fun decodesWhatAWatchSendsWithOptionalFieldsMissingAndUnknownKeys() {
        val decoded = assertNotNull(
            WatchWorkoutCodec.decode(
                """{"id":"w2","activity_type":"hiking","start_time":1,"end_time":2,"distance_meters":3.5,"extra":true}"""
            )
        )
        assertEquals(0.0, decoded.elevationGainMeters)
        assertNull(decoded.averageHeartRate)
        assertTrue(decoded.trackpoints.isEmpty())
    }

    @Test
    fun rejectsInvalidInput() {
        assertNull(WatchWorkoutCodec.decode("not json"))
        assertNull(WatchWorkoutCodec.decode("""{"id":"","activity_type":"x","start_time":1,"end_time":2,"distance_meters":0}"""))
        assertNull(WatchWorkoutCodec.decode("""{"id":"a","activity_type":"x","start_time":5,"end_time":2,"distance_meters":0}"""))
        assertNull(WatchWorkoutCodec.decode("""{"id":"a","activity_type":"x","start_time":1,"end_time":2,"distance_meters":0,"version":99}"""))
    }

    @Test
    fun inboxIgnoresDuplicatesAndInvalidJson() {
        val inbox = WatchWorkoutInbox()
        val acknowledged = mutableListOf<String>()
        inbox.onHandled = { acknowledged += it }

        assertTrue(inbox.offerJson(WatchWorkoutCodec.encode(workout)))
        assertTrue(inbox.offerJson(WatchWorkoutCodec.encode(workout)))
        assertEquals(false, inbox.offerJson("garbage"))
        assertEquals(1, inbox.pending.value.size)

        inbox.handled("w1")
        assertTrue(inbox.pending.value.isEmpty())
        assertEquals(listOf("w1"), acknowledged)
    }
}
