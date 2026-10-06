package com.wandr.android.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.wandr.domain.model.GpsTrackpoint
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/** GPS fixes from the fused location provider. The caller makes sure the location permission is granted. */
class FusedLocationSource(context: Context) {
    private val client = LocationServices.getFusedLocationProviderClient(context.applicationContext)

    /** A fix about every [intervalMillis] (never more often) for as long as the flow is collected. */
    @SuppressLint("MissingPermission")
    fun updates(intervalMillis: Long): Flow<Location> = callbackFlow {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMillis)
            .setMinUpdateIntervalMillis(intervalMillis)
            .setWaitForAccurateLocation(false)
            .build()
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.locations.forEach { trySend(it) }
            }
        }
        try {
            client.requestLocationUpdates(request, callback, Looper.getMainLooper())
        } catch (e: SecurityException) {
            close(e) // permission revoked while recording
        }
        awaitClose { client.removeLocationUpdates(callback) }
    }
}

fun Location.toTrackpoint() = GpsTrackpoint(
    latitude = latitude,
    longitude = longitude,
    altitudeMeters = if (hasAltitude()) altitude else 0.0,
    timestamp = time,
    speedMetersPerSecond = if (hasSpeed()) speed else 0f
)
