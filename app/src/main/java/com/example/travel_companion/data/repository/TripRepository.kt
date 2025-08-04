package com.example.travel_companion.data.repository

import androidx.lifecycle.LiveData
import com.example.travel_companion.data.local.dao.TripDao
import com.example.travel_companion.data.local.entity.TripEntity
import com.example.travel_companion.domain.model.TripStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TripRepository @Inject constructor(
    private val tripDao: TripDao
) {

    fun getAllTrips(): LiveData<List<TripEntity>> {
        return tripDao.getAll()
    }

    suspend fun addTrip(trip: TripEntity) {
        tripDao.insert(trip)
    }

    suspend fun updateTrip(trip: TripEntity) {
        tripDao.updateTrip(trip)
    }

    suspend fun deleteTrip(trip: TripEntity) {
        tripDao.deleteTrip(trip)
    }

    suspend fun getTripById(id: Long): TripEntity? {
        return tripDao.getTripById(id)
    }

    suspend fun isTripOverlapping(start: Long, end: Long): Boolean {
        return tripDao.getOverlappingTrips(start, end).isNotEmpty()
    }

    fun getTripsByStatus(status: TripStatus): LiveData<List<TripEntity>> {
        return tripDao.getTripsByStatus(status)
    }

    suspend fun updateTripStatuses() {
        val currentTime = System.currentTimeMillis()

        // Aggiorna i viaggi programmati che sono iniziati
        tripDao.updatePlannedTripsToStarted(currentTime)

        // Aggiorna i viaggi iniziati che sono finiti
        tripDao.updateStartedTripsToFinished(currentTime)
    }

    // Metodo per avviare il monitoraggio automatico degli stati
    fun startStatusMonitoring(scope: CoroutineScope) {
        scope.launch {
            while (true) {
                updateTripStatuses()
                delay(60_000) // Controlla ogni minuto
            }
        }
    }

    // Metodo per forzare un aggiornamento immediato degli stati
    suspend fun forceStatusUpdate() {
        updateTripStatuses()
    }
}