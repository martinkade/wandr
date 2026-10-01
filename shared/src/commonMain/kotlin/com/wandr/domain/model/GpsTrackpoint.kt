package com.wandr.domain.model

data class GpsTrackpoint(
    val latitude: Double,
    val longitude: Double,
    val altitudeMeters: Double,
    val timestamp: Long,
    val speedMetersPerSecond: Float = 0f
)
