package com.example.travel_companion.util

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.travel_companion.R
import com.example.travel_companion.presentation.ui.activity.MainActivity
import com.example.travel_companion.util.PermissionsManager.hasNotificationPermissions
import com.example.travel_companion.util.Utils.createNotificationChannel

class InactivityCheckWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {
        val sharedPrefs = applicationContext.getSharedPreferences("app_tracking", Context.MODE_PRIVATE)
        val lastActiveTime = sharedPrefs.getLong("last_tracking_time", System.currentTimeMillis())
        val currentTime = System.currentTimeMillis()

        val daysSinceLastUse = (currentTime - lastActiveTime) / (1000 * 60 * 60 * 24)

        // Check if user hasn't tracked a trip for 60 days
        if (daysSinceLastUse >= 60) {
            sendReEngagementNotification()
        }

        return Result.success()
    }

    private fun sendReEngagementNotification() {
        if (!hasNotificationPermissions(applicationContext)) {
            return
        }

        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        createNotificationChannel(notificationManager,CHANNEL_ID, CHANNEL_NAME)

        val intent = Intent(applicationContext, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("È tanto tempo che non tracci un viaggio!")
            .setContentText("Pianificane uno o inizia a tracciarne uno già iniziato!")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        private const val NOTIFICATION_ID = 1002
        private const val CHANNEL_ID = "re_engagement"
        private const val CHANNEL_NAME = "Re-engagement channel"
    }
}