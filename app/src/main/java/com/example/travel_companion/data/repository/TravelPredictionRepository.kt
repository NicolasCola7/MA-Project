package com.example.travel_companion.data.repository

import com.example.travel_companion.data.local.dao.TripDao
import com.example.travel_companion.data.local.dao.POIDao
import com.example.travel_companion.domain.model.TravelAnalysis
import com.example.travel_companion.domain.model.TripStatus
import com.example.travel_companion.util.TravelPredictionEngine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TravelPredictionRepository @Inject constructor(
    private val tripDao: TripDao,
    private val predictionEngine: TravelPredictionEngine
) {

    suspend fun generateTravelAnalysis(): TravelAnalysis {
        val trips = tripDao.getTripsByStatusSync(TripStatus.FINISHED)

        return predictionEngine.analyzeAndPredict(trips)
    }

    suspend fun getPredictionsForTripType(tripType: String): TravelAnalysis {
        val trips = tripDao.getTripsByStatusSync(TripStatus.FINISHED)
            .filter { it.type == tripType }

        return predictionEngine.analyzeAndPredict(trips)
    }
}