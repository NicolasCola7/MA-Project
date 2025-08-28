package com.example.travel_companion.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.travel_companion.data.repository.TripRepository
import com.example.travel_companion.domain.model.TripStatus
import com.example.travel_companion.util.trip.TripScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var tripScheduler: TripScheduler

    @Inject
    lateinit var tripRepository: TripRepository

    /**
     * Handles device boot completion by rescheduling active trips.
     */
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val pendingResult = goAsync()

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // Get data and pass them to the scheduler
                    val plannedTrips = tripRepository.getTripsByStatusSync(TripStatus.PLANNED)
                        .map { TripScheduler.TripData(it.id, it.startDate, it.endDate) }

                    val startedTrip = tripRepository.getTripsByStatusSync(TripStatus.STARTED)
                        .map { TripScheduler.TripData(it.id, it.startDate, it.endDate) }

                    tripScheduler.rescheduleActiveTrips(plannedTrips, startedTrip)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}