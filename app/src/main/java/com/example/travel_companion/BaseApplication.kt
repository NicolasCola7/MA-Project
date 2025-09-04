package com.example.travel_companion

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.travel_companion.util.workers.InactivityCheckWorker
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.PlacesClient
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Base application class for Travel Companion app.
 * Initializes logging, Google Places API, and schedules periodic inactivity checks.
 */
@HiltAndroidApp
class BaseApplication : Application() {

    /** Client for accessing Google Places API. */
    lateinit var placesClient: PlacesClient

    /**
     * Called when the application is starting, before any other application objects have been created.
     * Initializes Timber logging, Google Places API, and schedules inactivity checks.
     */
    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())

        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, "YOUR API KEY HERE")
        }

        placesClient = Places.createClient(this)
        scheduleInactivityCheck()
    }

    /**
     * Schedules a periodic worker to check for user inactivity.
     * The worker runs once every 7 days and requires network connectivity.
     */
    private fun scheduleInactivityCheck() {
        val workRequest = PeriodicWorkRequestBuilder<InactivityCheckWorker>(
            7, TimeUnit.DAYS // Check weekly
        ).setConstraints(
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "inactivity_check",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}
