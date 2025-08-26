package com.example.travel_companion.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.graphics.Bitmap
import android.os.Build
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

object Utils {
    const val TRACKING_TIME: Long = 1000
    val dateTimeFormat: SimpleDateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ITALY)

    fun resizeBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val originalWidth = bitmap.width
        val originalHeight = bitmap.height

        val scaleWidth = maxWidth.toFloat() / originalWidth
        val scaleHeight = maxHeight.toFloat() / originalHeight
        val scaleFactor = minOf(scaleWidth, scaleHeight)

        val newWidth = (originalWidth * scaleFactor).toInt()
        val newHeight = (originalHeight * scaleFactor).toInt()

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    fun getFormattedTrackingTime(timestamp: Long): String {
        var milliseconds = timestamp
        val hours = TimeUnit.MILLISECONDS.toHours(milliseconds)
        milliseconds -= TimeUnit.HOURS.toMillis(hours)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds)
        milliseconds -= TimeUnit.MINUTES.toMillis(minutes)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds)

        return "${if(hours < 10) "0" else ""}$hours:" +
                "${if(minutes < 10) "0" else ""}$minutes:" +
                "${if(seconds < 10) "0" else ""}$seconds"
    }

    fun createNotificationChannel(notificationManager: NotificationManager, channelId: String, channelName: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
}