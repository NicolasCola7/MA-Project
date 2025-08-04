package com.example.travel_companion.service

import com.example.travel_companion.data.repository.TripRepository
import kotlinx.coroutines.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TripMonitoringService @Inject constructor(
    private val tripRepository: TripRepository
) {
    private var monitoringJob: Job? = null

    fun startMonitoring(scope: CoroutineScope) {
        stopMonitoring() // Assicurati che non ci siano monitoraggi duplicati

        monitoringJob = scope.launch {
            while (isActive) {
                try {
                    updateTripStatuses()
                } catch (e: Exception) {
                    // Log dell'errore senza interrompere il monitoraggio
                    // Logger.e("TripStatusMonitoringService", "Error updating trip status", e)
                }
                delay(60_000) // ogni minuto
            }
        }
    }

    fun stopMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = null
    }

    suspend fun forceUpdate() {
        updateTripStatuses()
    }

    // Logica di business spostata qui dal Use Case
    private suspend fun updateTripStatuses() {
        val currentTime = System.currentTimeMillis()

        // Aggiorna i viaggi programmati che sono iniziati
        tripRepository.updatePlannedTripsToStarted(currentTime)

        // Aggiorna i viaggi iniziati che sono finiti
        tripRepository.updateStartedTripsToFinished(currentTime)
    }
}