package com.example.travel_companion.data.repository

import com.example.travel_companion.data.local.dao.TripDao
import com.example.travel_companion.domain.model.TripAnalysis
import com.example.travel_companion.domain.model.TripStatus
import com.example.travel_companion.util.TripPredictionEngine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TripPredictionRepository @Inject constructor(
    private val tripDao: TripDao,
    private val predictionEngine: TripPredictionEngine
) {

    suspend fun generateTripAnalysis(): TripAnalysis {
        val trips = tripDao.getTripsByStatusSync(TripStatus.FINISHED)

        return predictionEngine.analyzeAndPredict(trips)
    }

    suspend fun getPredictionsForTripType(tripType: String): TripAnalysis {
        val trips = tripDao.getTripsByStatusSync(TripStatus.FINISHED)
            .filter { it.type == tripType }

        return predictionEngine.analyzeAndPredict(trips)
    }
}