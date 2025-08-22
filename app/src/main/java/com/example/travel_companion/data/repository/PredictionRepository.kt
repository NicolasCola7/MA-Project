package com.example.travel_companion.data.repository

import androidx.lifecycle.asFlow
import com.example.travel_companion.data.local.dao.TripDao
import com.example.travel_companion.data.local.entity.TripEntity
import com.example.travel_companion.util.TripPredictionAlgorithm
import com.example.travel_companion.util.TripSuggestionsEngine
import com.example.travel_companion.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PredictionRepository @Inject constructor(
    private val tripDao: TripDao,
    private val predictionAlgorithm: TripPredictionAlgorithm,
    private val suggestionsEngine: TripSuggestionsEngine
) {

    suspend fun calculateTravelPrediction(): TripPrediction {
        val allTrips = getAllTripsSync()
        return predictionAlgorithm.calculatePrediction(allTrips)
    }

    suspend fun getTravelSuggestions(): List<TripSuggestion> {
        val allTrips = getAllTripsSync()
        val upcomingTrips = getTripsByStatusSync(TripStatus.PLANNED)
        val prediction = calculateTravelPrediction()

        return suggestionsEngine.generateSuggestions(allTrips, prediction, upcomingTrips)
    }

    fun getPredictionFlow(): Flow<TripPrediction> {
        return tripDao.getAll().asFlow().map { trips ->
            predictionAlgorithm.calculatePrediction(trips)
        }
    }

    // Metodi helper per ottenere dati sincroni
    private suspend fun getAllTripsSync(): List<TripEntity> {
        return try {
            tripDao.getAllTripsSync()
        } catch (e: Exception) {
            // Fallback: restituisci lista vuota se il metodo non esiste
            emptyList()
        }
    }

    private suspend fun getTripsByStatusSync(status: TripStatus): List<TripEntity> {
        return try {
            tripDao.getTripsByStatusSync(status)
        } catch (e: Exception) {
            emptyList()
        }
    }
}