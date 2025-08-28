package com.example.travel_companion.service

import android.annotation.SuppressLint

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import com.example.travel_companion.R
import com.example.travel_companion.receiver.GeofenceBroadcastReceiver
import com.example.travel_companion.util.Utils
import com.example.travel_companion.util.Utils.createNotificationChannel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

typealias Polyline = MutableList<LatLng>
typealias Polylines = MutableList<Polyline>

@AndroidEntryPoint
class TrackingService : LifecycleService() {

    lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    private lateinit var geofencingClient: GeofencingClient

    companion object {
        val trackingTimeInMillis = MutableLiveData<Long>()
        val isTracking = MutableLiveData<Boolean>()
        val pathPoints = MutableLiveData<Polylines>()
        val geofenceList = MutableLiveData<List<Geofence>>()

        const val NOTIFICATION_CHANNEL_ID = "tracking_channel"
        const val NOTIFICATION_CHANNEL_NAME = "Tracking"
        const val NOTIFICATION_ID = 1000
    }

    /**
     * Initializes the service, sets up location clients, and configures observers for tracking state changes.
     */
    override fun onCreate() {
        super.onCreate()

        isTracking.postValue(false)
        pathPoints.postValue(mutableListOf())

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        geofencingClient = LocationServices.getGeofencingClient(this)

        isTracking.observe(this) { // observes for changes in the tracking state
            updateLocationTracking(it)
            updateNotificationTrackingState(it)
        }

        // to update geofencing if the user adds/removes points when tracking is active
        geofenceList.observe(this) {
            if(isTracking.value == true) { //restart geofencing with updated geofences
                stopGeofencing()
                startGeofencing()
            }
        }
    }

    /**
     * Terminates the service by stopping tracking, removing foreground notification, and stopping geofencing.
     */
    private fun killService() {
        isTracking.postValue(false)
        pathPoints.postValue(mutableListOf())
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopGeofencing()
        stopSelf()
    }

    /**
     * Handles incoming service commands to start, pause, or stop the tracking service.
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when (it.action) {
                "ACTION_START_OR_RESUME_SERVICE" -> {
                    Timber.d("Starting service...")
                    startForegroundService()
                }

                "ACTION_PAUSE_SERVICE" -> {
                    Timber.d("Paused service")
                    pauseService()
                }

                "ACTION_STOP_SERVICE" -> {
                    Timber.d("Stopped service")
                    killService()
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private var lapTime = 0L
    private var timeRun = trackingTimeInMillis.value ?: 0L
    private var timeStarted = 0L

    /**
     * Starts the tracking timer and updates the elapsed time at regular intervals.
     */
    private fun startTimer() {
        addEmptyPolyline()
        isTracking.postValue(true)
        timeStarted = System.currentTimeMillis()
        CoroutineScope(Dispatchers.Main).launch {
            while (isTracking.value!!) {
                // time difference between now and timeStarted
                lapTime = System.currentTimeMillis() - timeStarted
                // post the new lapTime
                trackingTimeInMillis.postValue(timeRun + lapTime)

                delay(50L)
            }
            timeRun += lapTime
        }
    }

    /**
     * Pauses the tracking service by setting the tracking state to false.
     */
    private fun pauseService() {
        isTracking.postValue(false)
    }

    /**
     * Enables or disables location tracking and geofencing based on the tracking state.
     */
    @SuppressLint("MissingPermission")
    private fun updateLocationTracking(isTracking: Boolean) {
        if (isTracking) {
            val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, Utils.TRACKING_TIME)
                .setMinUpdateIntervalMillis(Utils.TRACKING_TIME)
                .build()

            fusedLocationProviderClient.requestLocationUpdates(
                request,
                locationCallback,
                Looper.getMainLooper()
            )

            startGeofencing()
        } else {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback)
            stopGeofencing()
        }
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            super.onLocationResult(result)
            if (isTracking.value!!) {
                result.locations.let { locations ->
                    for (location in locations) {
                        addPathPoint(location)
                        Timber.d("NEW LOCATION: ${location.latitude}, ${location.longitude}")
                    }
                }
            }
        }
    }

    /**
     * Adds a new location point to the current polyline if it differs from the previous point.
     */
    private fun addPathPoint(location: Location?) {
        location?.let {
            val pos = LatLng(location.latitude, location.longitude)
            pathPoints.value?.apply {
                if (isNotEmpty() && last().isNotEmpty() && areCoordinatesSame(last().last(), pos)) {
                    return
                }

                last().add(pos) // add new coordinate to the last polyline
                pathPoints.postValue(this)
            }
        }
    }

    /**
     * Creates a new empty polyline in the path points collection.
     */
    private fun addEmptyPolyline() = pathPoints.value?.apply {
        add(mutableListOf())
        pathPoints.postValue(this)
    } ?: pathPoints.postValue(mutableListOf(mutableListOf()))

    /**
     * Determines if two coordinates are within a specified distance threshold.
     */
    private fun areCoordinatesSame(
        pos1: LatLng,
        pos2: LatLng,
        thresholdMeters: Float = 5.0f
    ): Boolean {
        val result = FloatArray(1)
        Location.distanceBetween(
            pos1.latitude,
            pos1.longitude,
            pos2.latitude,
            pos2.longitude,
            result
        )
        return result[0] <= thresholdMeters
    }

    /**
     * Starts the service in foreground mode with a persistent notification.
     */
    private fun startForegroundService() {
        isTracking.postValue(true)
        startTimer()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel(notificationManager, NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME)

        startForeground(NOTIFICATION_ID, getBaseNotification().build())
    }

    /**
     * Creates the base notification builder with common notification properties.
     */
    private fun getBaseNotification() = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
        .setAutoCancel(false)
        .setOngoing(true)
        .setSmallIcon(R.drawable.ic_directions_run_black_24dp)
        .setContentTitle("Tracciamento")
        .setContentText("Tracciando la tua posizione")
        .setDeleteIntent(null)

    /**
     * Updates the notification text based on the current tracking state.
     */
    private fun updateNotificationTrackingState(isTracking: Boolean) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = getBaseNotification()
            .setContentText(
                if (isTracking) "Tracciando la tua posizione"
                else "Tracciamento in pausa"
            )
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    /**
     * Constructs a geofencing request from the current list of geofences.
     */
    private fun getGeofencingRequest(): GeofencingRequest? {
        val geofences = geofenceList.value
        if (geofences.isNullOrEmpty()) return null

        return GeofencingRequest.Builder().apply {
            setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            addGeofences(geofences)
        }.build()
    }

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(this, GeofenceBroadcastReceiver::class.java)
        intent.action = "com.google.android.gms.location.Geofence"
        PendingIntent.getBroadcast(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    /**
     * Activates geofencing monitoring using the configured geofences.
     */
    @SuppressLint("MissingPermission")
    private fun startGeofencing() {
        val request = getGeofencingRequest() ?: return

        geofencingClient.addGeofences(request, geofencePendingIntent).run {
            addOnSuccessListener { Timber.d("Geofencing started") }
            addOnFailureListener { Timber.d("Geofencing failed: ${it.message}") }
        }
    }

    /**
     * Deactivates geofencing monitoring and removes all registered geofences.
     */
    private fun stopGeofencing() {
        geofencingClient.removeGeofences(geofencePendingIntent).run {
            addOnSuccessListener {
                Timber.d("Geofencing stopped")
            }
            addOnFailureListener {
                Timber.d("Geofencing stop failed ${it.message}")
            }
        }
    }

    /**
     * Cleans up resources and cancels the notification when the service is destroyed.
     */
    override fun onDestroy() {
        super.onDestroy()
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
    }
}