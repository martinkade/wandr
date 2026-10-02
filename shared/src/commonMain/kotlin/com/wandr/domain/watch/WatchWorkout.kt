package com.wandr.domain.watch

import com.wandr.domain.model.GpsTrackpoint
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Origins of an externally recorded [WatchWorkout]. */
object WorkoutSource {
    const val WATCH = "watch"
    const val HEALTH_CONNECT = "health_connect"
    const val APPLE_HEALTH = "apple_health"
}

/**
 * A workout recorded outside the app and imported into it: recorded on a companion watch (WearOS / WatchOS) and sent
 * to the phone as JSON ([WatchWorkoutCodec]), or read from Health Connect / Apple Health. The [id] is a UUID that stays
 * the same for the same workout, so a workout that is delivered twice is only imported once.
 */
@Serializable
data class WatchWorkout(
    val id: String,
    /** `hiking`, `running` or `cycling` (see the app's activity types). */
    @SerialName("activity_type") val activityType: String,
    /** Epoch milliseconds. */
    @SerialName("start_time") val startTime: Long,
    @SerialName("end_time") val endTime: Long,
    @SerialName("distance_meters") val distanceMeters: Double,
    @SerialName("elevation_gain_meters") val elevationGainMeters: Double = 0.0,
    @SerialName("average_heart_rate") val averageHeartRate: Int? = null,
    @SerialName("max_heart_rate") val maxHeartRate: Int? = null,
    val trackpoints: List<WatchTrackpoint> = emptyList(),
    /** Where the workout comes from, see [WorkoutSource]. Watches do not send it. */
    val source: String = WorkoutSource.WATCH,
    val version: Int = WatchWorkoutCodec.VERSION
)

@Serializable
data class WatchTrackpoint(
    @SerialName("lat") val latitude: Double,
    @SerialName("lon") val longitude: Double,
    @SerialName("alt") val altitudeMeters: Double = 0.0,
    /** Epoch milliseconds. */
    @SerialName("t") val timestamp: Long,
    @SerialName("speed") val speedMetersPerSecond: Float = 0f
) {
    fun toGpsTrackpoint() = GpsTrackpoint(latitude, longitude, altitudeMeters, timestamp, speedMetersPerSecond)
}

/** The wire format shared by both watch apps and the phone apps (snake_case JSON, unknown keys are ignored). */
object WatchWorkoutCodec {
    const val VERSION = 1

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(workout: WatchWorkout): String = json.encodeToString(WatchWorkout.serializer(), workout)

    /** Null if [text] is not a valid workout, e.g. garbage or an unsupported newer format. */
    fun decode(text: String): WatchWorkout? = runCatching { json.decodeFromString(WatchWorkout.serializer(), text) }
        .getOrNull()
        ?.takeIf { it.version <= VERSION && it.id.isNotBlank() && it.endTime >= it.startTime }
}
