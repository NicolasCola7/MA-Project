package com.example.travel_companion.util

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
/*
    CLASSE DEDICATA ALLA SCHEDULAZIONE DI VIAGGI. Ciò serve per ottimizzare la soluzione rispetto all'utilizzo
    di un semplice polling. Facendo così infatti, l’app sa già quando dovrà aggiornare lo stato, e lo dice ad Android subito.

    Il metodo scheduleTrip, chiamato dalla classe TripManagerservice, ha il compito di schedulare un viaggio.
    La schedulazione viene svolta tramite la creazione di due allarmi:
        - uno per l'inizio
        - uno per la fine
    Gli allarmi sono gestiti tramite AlertManager. In particolare l'allarme scatta alla data e ora precisa
    anche se il telefono è in modalità Doze (risparmio energetico).
    Per ogni viaggio vengono creati Intent differenti per inizio e fine.
    Nel momento in cui scatta l'allarme, Android invia un broadcast a TripStatusReceiver svegliando così l’app esattamente
    al momento giusto, senza bisogno di fare polling.
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

    fun scheduleTrip(tripId: Long, startTime: Long, endTime: Long) {
        // Cancella eventuali alarm esistenti per questo trip
        cancelTripAlarms(listOf(tripId))

        val currentTime = System.currentTimeMillis()

        // Schedula inizio se non è già passato
        if (startTime > currentTime) {
            scheduleStatusUpdate(tripId, startTime, TripStatus.STARTED)
        }

        // Schedula fine se non è già passata
        if (endTime > currentTime) {
            scheduleStatusUpdate(tripId, endTime, TripStatus.FINISHED)
        }
    }

    fun cancelTripAlarms(tripIds: List<Long>) {
        if (tripIds.isEmpty()) return

        val pendingIntentsToCancel = mutableListOf<PendingIntent>()

        // Prepara tutti i PendingIntent
        tripIds.forEach { tripId ->
            // PendingIntent per inizio
            val startIntent = createStatusUpdateIntent(tripId, TripStatus.STARTED)
            val startPendingIntent = PendingIntent.getBroadcast(
                context,
                "start_$tripId".hashCode(),
                startIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            pendingIntentsToCancel.add(startPendingIntent)

            // PendingIntent per fine
            val endIntent = createStatusUpdateIntent(tripId, TripStatus.FINISHED)
            val endPendingIntent = PendingIntent.getBroadcast(
                context,
                "end_$tripId".hashCode(),
                endIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            pendingIntentsToCancel.add(endPendingIntent)
        }

        // Cancella tutti gli allarmi
        pendingIntentsToCancel.forEach { pendingIntent ->
            alarmManager.cancel(pendingIntent)
        }
    }

    @SuppressLint("MissingPermission")
    private  fun scheduleStatusUpdate(tripId: Long, triggerTime: Long, status: TripStatus) {
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

    private fun createStatusUpdateIntent(tripId: Long, status: TripStatus): Intent {
        return Intent(context, TripStatusReceiver::class.java).apply {
            action = ACTION_UPDATE_TRIP_STATUS
            putExtra(EXTRA_TRIP_ID, tripId)
            putExtra(EXTRA_NEW_STATUS, status.name)
        }
    }

    // Metodo per rischedulare tutti i trip attivi - ora prende i dati come parametro
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

    // Data class semplice per evitare dipendenze
    data class TripData(val id: Long, val startDate: Long, val endDate: Long)
}