package com.tsugiba.nav.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.tsugiba.nav.R
import com.tsugiba.nav.ui.MainActivity

class LocationTrackingService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    companion object {
        const val ACTION_LOCATION_UPDATE = "com.tsugiba.nav.LOCATION_UPDATE"
        const val EXTRA_LAT = "lat"
        const val EXTRA_LNG = "lng"
        private const val CHANNEL_ID = "tsugiba_location"
        private const val NOTIFICATION_ID = 1001
        private const val UPDATE_INTERVAL_MS = 5_000L
        private const val FASTEST_INTERVAL_MS = 3_000L
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        startLocationUpdates()
    }

    private fun startLocationUpdates() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { loc ->
                    Intent(ACTION_LOCATION_UPDATE).also {
                        it.putExtra(EXTRA_LAT, loc.latitude)
                        it.putExtra(EXTRA_LNG, loc.longitude)
                        sendBroadcast(it)
                    }
                }
            }
        }

        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            UPDATE_INTERVAL_MS
        ).setMinUpdateIntervalMillis(FASTEST_INTERVAL_MS).build()

        try {
            fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
        } catch (e: SecurityException) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.notification_tracking))
            .setSmallIcon(R.drawable.ic_walking)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Location Tracking",
            NotificationManager.IMPORTANCE_LOW
        ).apply { description = "Active navigation tracking" }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
