package com.wandr.domain.geo

/** How densely a recording samples the GPS, depending on the sport. */
object RecordingPolicy {
    /** Fast sports (cycling, running, ...): one data point per second. */
    const val FAST_INTERVAL_MILLIS = 1_000L

    /** Walking and hiking: one data point every 3 seconds is enough and saves battery and storage. */
    const val SLOW_INTERVAL_MILLIS = 3_000L

    fun sampleIntervalMillis(activityType: String): Long = when (activityType) {
        "hiking", "walking" -> SLOW_INTERVAL_MILLIS
        else -> FAST_INTERVAL_MILLIS
    }

    /**
     * Fixes closer than this to the last recorded point are dropped: location providers may deliver faster than
     * requested. A little tolerance keeps a fix that arrives a few milliseconds early.
     */
    fun minGapMillis(activityType: String): Long = (sampleIntervalMillis(activityType) * 0.9).toLong()
}
