package com.example.travel_companion.service

import com.example.travel_companion.data.local.entity.TripEntity
import com.example.travel_companion.data.repository.TripRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TripManagerService @Inject constructor(
    private val tripRepository: TripRepository,
    private val tripScheduler: TripScheduler
) {

    suspend fun addTrip(trip: TripEntity): Long {
        val tripId = tripRepository.addTrip(trip)
        // Schedula dopo aver aggiunto
        tripScheduler.scheduleTrip(tripId, trip.startDate, trip.endDate)
        return tripId
    }

    suspend fun updateTrip(trip: TripEntity) {
        tripRepository.updateTrip(trip)
        // Rischedula con i nuovi orari
        tripScheduler.scheduleTrip(trip.id, trip.startDate, trip.endDate)
    }

    suspend fun deleteTrip(tripId: Long) {
        // Cancella scheduling prima di eliminare
        tripScheduler.cancelTripAlarms(tripId)
        tripRepository.deleteTrip(tripId)
    }

    suspend fun forceUpdateAllStatuses() {
        tripRepository.forceUpdateAllStatuses()
    }
}