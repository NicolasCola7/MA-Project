package com.example.travel_companion.domain.usecase

import com.example.travel_companion.data.repository.TripRepository
import com.example.travel_companion.domain.model.TripStatus
import javax.inject.Inject

class UpdateTripStatusUseCase @Inject constructor(
    private val tripRepository: TripRepository
) {
    suspend operator fun invoke() {
        val currentTime = System.currentTimeMillis()

        // Aggiorna i viaggi programmati che sono iniziati
        tripRepository.updatePlannedTripsToStarted(currentTime)

        // Aggiorna i viaggi iniziati che sono finiti
        tripRepository.updateStartedTripsToFinished(currentTime)
    }
}