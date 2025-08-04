package com.example.travel_companion.service

import com.example.travel_companion.data.local.entity.TripEntity
import com.example.travel_companion.data.repository.TripRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TripStatusService @Inject constructor(
    private val tripRepository: TripRepository,
    private val tripScheduler: TripScheduler
) {

    // Metodo per aggiungere un nuovo trip e schedulare i suoi aggiornamenti
    suspend fun addTripWithScheduling(trip: TripEntity) {
        val tripId = tripRepository.addTrip(trip)
        tripScheduler.scheduleTrip(trip.id, trip.startDate, trip.endDate)
    }

    // Metodo per aggiornare un trip esistente
    suspend fun updateTripWithScheduling(trip: TripEntity) {
        tripRepository.updateTrip(trip)
        tripScheduler.scheduleTrip(trip.id, trip.startDate, trip.endDate)
    }

    // Metodo per eliminare un trip
    suspend fun deleteTripWithScheduling(trip: TripEntity) {
        tripScheduler.cancelTripAlarms(trip.id)
        tripRepository.deleteTrip(trip)
    }

    // Forza aggiornamento immediato di tutti gli stati (per debug o casi edge)
    suspend fun forceUpdateAllStatuses() {
        val currentTime = System.currentTimeMillis()
        tripRepository.updatePlannedTripsToStarted(currentTime)
        tripRepository.updateStartedTripsToFinished(currentTime)
    }
}