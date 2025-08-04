package com.example.travel_companion.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.travel_companion.data.repository.TripRepository
import com.example.travel_companion.domain.model.TripStatus
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

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val pendingResult = goAsync()

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // Ottieni i dati e passa al scheduler
                    val plannedTrips = tripRepository.getTripsByStatusSync(TripStatus.PLANNED)
                        .map { TripScheduler.TripData(it.id, it.startDate, it.endDate) }

                    val startedTrips = tripRepository.getTripsByStatusSync(TripStatus.STARTED)
                        .map { TripScheduler.TripData(it.id, it.startDate, it.endDate) }

                    tripScheduler.rescheduleActiveTrips(plannedTrips, startedTrips)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}