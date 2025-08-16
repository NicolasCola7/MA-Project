package com.example.travel_companion.data.repository

import com.example.travel_companion.data.local.dao.POIDao
import com.example.travel_companion.data.local.entity.POIEntity
import javax.inject.Inject

class POIRepository @Inject constructor(
    private val poiDao: POIDao
) {

    fun getPOIs(tripId: Long): List<POIEntity> {
        return poiDao.getPOIs(tripId)
    }

    suspend fun insertPOI(poi: POIEntity) {
        poiDao.insertPOI(poi)
    }

    fun deletePOI(poiName: String, tripId: Long) {
        poiDao.deletePOI(poiName, tripId)
    }
}