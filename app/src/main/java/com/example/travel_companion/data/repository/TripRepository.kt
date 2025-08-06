package com.example.travel_companion.data.repository

import androidx.lifecycle.LiveData
import com.example.travel_companion.data.local.dao.TripDao
import com.example.travel_companion.data.local.entity.TripEntity
import com.example.travel_companion.domain.model.TripStatus
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TripRepository @Inject constructor(
    private val tripDao: TripDao
) {

    fun getAllTrips(): LiveData<List<TripEntity>> {
        return tripDao.getAll()
    }

    suspend fun addTrip(trip: TripEntity): Long {
        return tripDao.insert(trip)
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

    suspend fun updateTripStatus(tripId: Long, status: TripStatus) {
        tripDao.updateTripStatus(tripId, status)
    }

    fun getTripsByStatus(status: TripStatus): LiveData<List<TripEntity>> {
        return tripDao.getTripsByStatus(status)
    }

    suspend fun getTripsByStatusSync(status: TripStatus): List<TripEntity> {
        return tripDao.getTripsByStatusSync(status)
    }

    suspend fun updatePlannedTripsToStarted(currentTime: Long): Int {
        return tripDao.updatePlannedTripsToStarted(currentTime)
    }

    suspend fun updateStartedTripsToFinished(currentTime: Long): Int {
        return tripDao.updateStartedTripsToFinished(currentTime)
    }

    suspend fun forceUpdateAllStatuses() {
        val currentTime = System.currentTimeMillis()
        updatePlannedTripsToStarted(currentTime)
        updateStartedTripsToFinished(currentTime)
    }

}