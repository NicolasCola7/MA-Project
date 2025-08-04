package com.example.travel_companion.service

import com.example.travel_companion.domain.usecase.UpdateTripStatusUseCase
import kotlinx.coroutines.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TripStatusMonitoringService @Inject constructor(
    private val updateTripStatusUseCase: UpdateTripStatusUseCase
) {
    private var monitoringJob: Job? = null

    fun startMonitoring(scope: CoroutineScope) {
        stopMonitoring() // Assicurati che non ci siano monitoraggi duplicati

        monitoringJob = scope.launch {
            while (isActive) {
                try {
                    updateTripStatusUseCase()
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
        updateTripStatusUseCase()
    }
}