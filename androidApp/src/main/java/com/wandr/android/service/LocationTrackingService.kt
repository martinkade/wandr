package com.wandr.android.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.wandr.android.MainActivity
import com.wandr.android.R
import com.wandr.android.location.FusedLocationSource
import com.wandr.android.location.toTrackpoint
import com.wandr.android.ui.activity.ActivityFormat
import com.wandr.di.RecordingScope
import com.wandr.domain.geo.RecordingPolicy
import com.wandr.presentation.activity.ActivityIntent
import com.wandr.presentation.activity.ActivityState
import com.wandr.presentation.activity.ActivityViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koin.mp.KoinPlatform

/**
 * Keeps a recording alive while the app is in the background: a foreground service with an ongoing notification
 * (live distance and time). It reads the GPS (one fix per second for fast sports, every 3 seconds for hiking), feeds the
 * fixes into the process-wide recording view model and ticks its clock. It stops itself when the recording ends.
 */
class LocationTrackingService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var job: Job? = null
    private var lastNotificationText: String? = null

    private val recording: ActivityViewModel by lazy { KoinPlatform.getKoin().get(RecordingScope) }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // The notification's button: pause or resume (the recording screen does the same through the view model).
        when (intent?.action) {
            ACTION_PAUSE -> if (recording.state.value.isTracking) recording.processIntent(ActivityIntent.PauseGpsTracking)
            ACTION_RESUME -> if (recording.state.value.isTracking) recording.processIntent(ActivityIntent.ResumeGpsTracking)
        }
        // A foreground service must show its notification right away.
        val type = recording.state.value.trackingActivityType
        ServiceCompat.startForeground(
            this, NOTIFICATION_ID, buildNotification(recording.state.value),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION else 0
        )

        val permitted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!permitted || !recording.state.value.isTracking) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (job == null) {
            job = scope.launch {
                launch {
                    runCatching {
                        FusedLocationSource(this@LocationTrackingService)
                            .updates(RecordingPolicy.sampleIntervalMillis(type))
                            .collect { location ->
                                recording.processIntent(ActivityIntent.GpsFixChanged(location.accuracy))
                                recording.processIntent(ActivityIntent.AddTrackpoint(location.toTrackpoint()))
                            }
                    }
                }
                // The clock counts seconds even when no fix arrives (e.g. in a tunnel).
                while (isActive) {
                    delay(CLOCK_MILLIS)
                    val state = recording.state.value
                    if (!state.isTracking) {
                        stopSelf()
                        break
                    }
                    recording.processIntent(ActivityIntent.Tick(System.currentTimeMillis()))
                    updateNotification(recording.state.value)
                }
            }
        }
        // After a process death the recording is gone, so the service must not come back on its own.
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun notificationText(state: ActivityState): String =
        "${ActivityFormat.distanceKm(state.liveDistanceMeters)} km · ${ActivityFormat.clock(state.liveDurationSeconds)}"

    /**
     * The ongoing notification, shown as a Live Update on Android 16+ (a chip in the status bar with the recording time, and
     * on the lock screen): the progress bar fills towards the next full kilometer. Older versions show a plain ongoing
     * notification with a progress bar.
     */
    private fun buildNotification(state: ActivityState) = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle(getString(if (state.isPaused) R.string.recording_notification_paused else R.string.recording_notification_title))
        .setContentText(notificationText(state))
        .setSmallIcon(R.drawable.ic_record)
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setCategory(NotificationCompat.CATEGORY_WORKOUT)
        .setStyle(
            NotificationCompat.ProgressStyle()
                .addProgressSegment(NotificationCompat.ProgressStyle.Segment(METERS_PER_KM))
                .setProgress((state.liveDistanceMeters % METERS_PER_KM).toInt())
                .setStyledByProgress(false)
        )
        .setRequestPromotedOngoing(true)
        .setShortCriticalText(if (state.isPaused) getString(R.string.recording_chip_paused) else ActivityFormat.clock(state.liveDurationSeconds))
        .addAction(
            if (state.isPaused) R.drawable.ic_play_arrow else R.drawable.ic_pause,
            getString(if (state.isPaused) R.string.resume_button else R.string.pause_button),
            PendingIntent.getService(
                this, 1,
                Intent(this, LocationTrackingService::class.java).setAction(if (state.isPaused) ACTION_RESUME else ACTION_PAUSE),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        )
        .setContentIntent(
            PendingIntent.getActivity(
                this, 0, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        )
        .build()

    private fun updateNotification(state: ActivityState) {
        val text = "${state.isPaused}${notificationText(state)}"
        if (text == lastNotificationText) return
        lastNotificationText = text
        getSystemService(NotificationManager::class.java)?.notify(NOTIFICATION_ID, buildNotification(state))
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, getString(R.string.recording_channel_name), NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    private companion object {
        const val CHANNEL_ID = "wandr_location_channel"
        const val NOTIFICATION_ID = 1001
        const val CLOCK_MILLIS = 1_000L
        const val METERS_PER_KM = 1_000
        const val ACTION_PAUSE = "com.wandr.android.action.PAUSE_RECORDING"
        const val ACTION_RESUME = "com.wandr.android.action.RESUME_RECORDING"
    }
}
