package com.example.travel_companion

import android.app.Application
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.PlacesClient
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

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
    }
}