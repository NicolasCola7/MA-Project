package com.example.travel_companion.receiver

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.travel_companion.R
import com.example.travel_companion.presentation.ui.activity.MainActivity
import com.example.travel_companion.receiver.TripStatusReceiver.Companion
import com.example.travel_companion.util.PermissionsManager.hasNotificationPermissions
import com.example.travel_companion.util.Utils.createNotificationChannel
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import timber.log.Timber

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    companion object {
        private const val NOTIFICATION_ID = 1004
        private const val CHANNEL_ID = "geofence_channel"
        private const val CHANNEL_NAME = "Geofence"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent!!)
        if (geofencingEvent!!.hasError()) {
            val errorMessage = GeofenceStatusCodes
                .getStatusCodeString(geofencingEvent.errorCode)
            Timber.d("gefencing error: $errorMessage")
            return
        }

        val geofenceTransition = geofencingEvent.geofenceTransition

        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ||
            geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {

            // Get the geofences that were triggered. A single event can trigger
            // multiple geofences.
            val triggeringGeofences = geofencingEvent.triggeringGeofences

            sendNotification(context!!, geofenceTransition)
        }
    }

    private fun sendNotification(context: Context, geofenceTransition: Int) {
        if (!hasNotificationPermissions(context)) {
            return
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        createNotificationChannel(notificationManager, CHANNEL_ID, CHANNEL_NAME)

        val transition = if (geofenceTransition == 1) "entrando in" else "uscendo da"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Geofencing")
            .setContentText("Stai $transition una zona di interesse!")
            .setSmallIcon(R.drawable.ic_notification)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}