package com.example.travel_companion.data.repository

import androidx.lifecycle.LiveData
import com.example.travel_companion.data.local.dao.TripDao
import com.example.travel_companion.data.local.entity.TripEntity
import javax.inject.Inject

class TripRepository @Inject constructor(
    private val tripDao: TripDao
)  {

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
}