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

@HiltAndroidApp
class BaseApplication: Application() {
    lateinit var placesClient: PlacesClient

    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())

        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, "AIzaSyDEVW0HX64ZlwkoVMAZVr7OqgKO4IAuWno")
        }

        placesClient = Places.createClient(this)
        scheduleInactivityCheck()
    }

    private fun scheduleInactivityCheck() {
        val workRequest = PeriodicWorkRequestBuilder<InactivityCheckWorker>(
            7, TimeUnit.DAYS  // Check weekly
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