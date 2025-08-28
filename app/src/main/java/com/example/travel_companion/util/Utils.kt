package com.example.travel_companion.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.graphics.Bitmap
import android.os.Build
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Utility object providing general-purpose methods for bitmap resizing,
 * time formatting, and notification channel creation.
 */
object Utils {

    /** Interval in milliseconds used for tracking purposes. */
    const val TRACKING_TIME: Long = 1000

    /** Date and time formatter using the pattern "dd/MM/yyyy HH:mm" for Italy locale. */
    val dateTimeFormat: SimpleDateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ITALY)

    /**
     * Resizes a given bitmap while maintaining its aspect ratio.
     *
     * @param bitmap The original bitmap to resize.
     * @param maxWidth Maximum width of the resulting bitmap.
     * @param maxHeight Maximum height of the resulting bitmap.
     * @return A new bitmap scaled to fit within the specified dimensions.
     */
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

    /**
     * Converts a timestamp in milliseconds to a formatted string "HH:mm:ss".
     *
     * @param timestamp The time duration in milliseconds.
     * @return A string representing the formatted hours, minutes, and seconds.
     */
    fun getFormattedTrackingTime(timestamp: Long): String {
        var milliseconds = timestamp
        val hours = TimeUnit.MILLISECONDS.toHours(milliseconds)
        milliseconds -= TimeUnit.HOURS.toMillis(hours)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds)
        milliseconds -= TimeUnit.MINUTES.toMillis(minutes)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds)

        return "${if (hours < 10) "0" else ""}$hours:" +
                "${if (minutes < 10) "0" else ""}$minutes:" +
                "${if (seconds < 10) "0" else ""}$seconds"
    }

    /**
     * Creates a notification channel on devices running Android O or higher.
     *
     * @param notificationManager The NotificationManager to register the channel with.
     * @param channelId Unique identifier for the notification channel.
     * @param channelName User-visible name for the notification channel.
     */
    fun createNotificationChannel(
        notificationManager: NotificationManager,
        channelId: String,
        channelName: String
    ) {
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
