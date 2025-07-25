package com.example.travel_companion.data.repository

import com.example.travel_companion.data.local.dao.CoordinateDao
import com.example.travel_companion.data.local.entity.CoordinateEntity
import javax.inject.Inject

class CoordinateRepository @Inject constructor(
    private val coordinateDao: CoordinateDao
) {

    fun getCoordinatesForTrip(tripId: Long): List<CoordinateEntity> {
        return coordinateDao.getCoordinatesForTrip(tripId)
    }

    suspend fun insertCoordinate(coordinate: CoordinateEntity) {
        coordinateDao.insertCoordinate(coordinate)
    }
}