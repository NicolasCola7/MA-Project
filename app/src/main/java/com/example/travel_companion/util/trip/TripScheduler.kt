package com.example.travel_companion.util.trip

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.travel_companion.domain.model.TripStatus
import com.example.travel_companion.receiver.TripStatusReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility class for scheduling and managing trip status updates using AlarmManager.
 * Handles automatic transition of trips to STARTED or FINISHED states at specified times.
 */
@Singleton
class TripScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    companion object {
        const val ACTION_UPDATE_TRIP_STATUS = "com.example.travel_companion.UPDATE_TRIP_STATUS"
        const val EXTRA_TRIP_ID = "trip_id"
        const val EXTRA_NEW_STATUS = "new_status"
    }

    /**
     * Schedules start and end alarms for a trip.
     * Cancels any previously scheduled alarms for the same trip.
     *
     * @param tripId The unique identifier of the trip.
     * @param startTime Timestamp (ms) when the trip should start.
     * @param endTime Timestamp (ms) when the trip should finish.
     */
    fun scheduleTrip(tripId: Long, startTime: Long, endTime: Long) {
        cancelTripAlarms(listOf(tripId))

        val currentTime = System.currentTimeMillis()

        if (startTime > currentTime) {
            scheduleStatusUpdate(tripId, startTime, TripStatus.STARTED)
        }

        if (endTime > currentTime) {
            scheduleStatusUpdate(tripId, endTime, TripStatus.FINISHED)
        }
    }

    /**
     * Cancels all scheduled alarms (start and end) for the provided trip IDs.
     *
     * @param tripIds List of trip identifiers whose alarms should be canceled.
     */
    fun cancelTripAlarms(tripIds: List<Long>) {
        if (tripIds.isEmpty()) return

        val pendingIntentsToCancel = mutableListOf<PendingIntent>()

        tripIds.forEach { tripId ->
            val startIntent = createStatusUpdateIntent(tripId, TripStatus.STARTED)
            val startPendingIntent = PendingIntent.getBroadcast(
                context,
                "start_$tripId".hashCode(),
                startIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            pendingIntentsToCancel.add(startPendingIntent)

            val endIntent = createStatusUpdateIntent(tripId, TripStatus.FINISHED)
            val endPendingIntent = PendingIntent.getBroadcast(
                context,
                "end_$tripId".hashCode(),
                endIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            pendingIntentsToCancel.add(endPendingIntent)
        }

        pendingIntentsToCancel.forEach { pendingIntent ->
            alarmManager.cancel(pendingIntent)
        }
    }

    /**
     * Schedules a single alarm for updating the trip status at a specified time.
     *
     * @param tripId The unique identifier of the trip.
     * @param triggerTime Timestamp (ms) when the status update should occur.
     * @param status The TripStatus to be set at the trigger time.
     */
    @SuppressLint("MissingPermission")
    private fun scheduleStatusUpdate(tripId: Long, triggerTime: Long, status: TripStatus) {
        val intent = createStatusUpdateIntent(tripId, status)
        val requestCode = when (status) {
            TripStatus.STARTED -> "start_$tripId".hashCode()
            TripStatus.FINISHED -> "end_$tripId".hashCode()
            else -> return
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerTime,
            pendingIntent
        )
    }

    /**
     * Creates an Intent to broadcast a trip status update.
     *
     * @param tripId The unique identifier of the trip.
     * @param status The TripStatus to broadcast.
     * @return Configured Intent for the TripStatusReceiver.
     */
    private fun createStatusUpdateIntent(tripId: Long, status: TripStatus): Intent {
        return Intent(context, TripStatusReceiver::class.java).apply {
            action = ACTION_UPDATE_TRIP_STATUS
            putExtra(EXTRA_TRIP_ID, tripId)
            putExtra(EXTRA_NEW_STATUS, status.name)
        }
    }

    /**
     * Reschedules all active trips, both planned and already started.
     * Ensures that alarms are set for future start or end times.
     *
     * @param plannedTrips List of trips not yet started.
     * @param startedTrips List of trips already started but not finished.
     */
    fun rescheduleActiveTrips(plannedTrips: List<TripData>, startedTrips: List<TripData>) {
        val currentTime = System.currentTimeMillis()

        plannedTrips.forEach { trip ->
            if (trip.startDate > currentTime || trip.endDate > currentTime) {
                scheduleTrip(trip.id, trip.startDate, trip.endDate)
            }
        }

        startedTrips.forEach { trip ->
            if (trip.endDate > currentTime) {
                scheduleStatusUpdate(trip.id, trip.endDate, TripStatus.FINISHED)
            }
        }
    }

    /**
     * Simple data class representing trip timing information.
     *
     * @param id Unique trip identifier.
     * @param startDate Start timestamp (ms) of the trip.
     * @param endDate End timestamp (ms) of the trip.
     */
    data class TripData(val id: Long, val startDate: Long, val endDate: Long)
}
