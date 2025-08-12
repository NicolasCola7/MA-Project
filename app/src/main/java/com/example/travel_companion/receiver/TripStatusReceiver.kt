package com.example.travel_companion.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.travel_companion.R
import com.example.travel_companion.data.repository.TripRepository
import com.example.travel_companion.domain.model.TripStatus
import com.example.travel_companion.presentation.ui.activity.MainActivity
import com.example.travel_companion.util.PermissionsManager.hasNotificationPermissions
import com.example.travel_companion.util.TripScheduler
import com.example.travel_companion.util.Utils.createNotificationChannel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class TripStatusReceiver : BroadcastReceiver() {

    @Inject
    lateinit var tripRepository: TripRepository

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "trip_status_channel"
        private const val CHANNEL_NAME = "Trip Status"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == TripScheduler.ACTION_UPDATE_TRIP_STATUS) {
            val tripId = intent.getLongExtra(TripScheduler.EXTRA_TRIP_ID, -1)
            val statusName = intent.getStringExtra(TripScheduler.EXTRA_NEW_STATUS)

            if (tripId != -1L && statusName != null) {
                val status = TripStatus.valueOf(statusName)

                // Usa goAsync() per operazioni asincrone
                val pendingResult = goAsync()

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        tripRepository.updateTripStatus(tripId, status)
                    } finally {
                        pendingResult.finish()
                    }
                }

                if (status == TripStatus.STARTED) {
                    sendNotification(context, tripId)
                }
            }
        }
    }

    private fun sendNotification(context: Context, tripId: Long) {
        if (!hasNotificationPermissions(context)) {
            return
        }


        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        createNotificationChannel(notificationManager, CHANNEL_ID, CHANNEL_NAME)

        // Create intent to open app when notification is tapped
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("trip_id", tripId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            tripId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Viaggio iniziato")
            .setContentText("Un viaggio è appena iniziato, inizia a tracciarlo ora!")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}