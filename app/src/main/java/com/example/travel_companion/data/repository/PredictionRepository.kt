package com.example.travel_companion.data.repository

import androidx.lifecycle.asFlow
import com.example.travel_companion.data.local.dao.TripDao
import com.example.travel_companion.data.local.entity.TripEntity
import com.example.travel_companion.util.TravelPredictionAlgorithm
import com.example.travel_companion.util.TravelSuggestionsEngine
import com.example.travel_companion.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PredictionRepository @Inject constructor(
    private val tripDao: TripDao,
    private val predictionAlgorithm: TravelPredictionAlgorithm,
    private val suggestionsEngine: TravelSuggestionsEngine
) {

    suspend fun calculateTravelPrediction(): TravelPrediction {
        // Converti LiveData in lista usando value (funziona solo se già osservato)
        // Oppure usa una query sincrona se disponibile nel DAO
        val allTrips = getAllTripsSync()
        return predictionAlgorithm.calculatePrediction(allTrips)
    }

    suspend fun getTravelSuggestions(): List<TravelSuggestion> {
        val allTrips = getAllTripsSync()
        val upcomingTrips = getTripsByStatusSync(TripStatus.PLANNED)
        val prediction = calculateTravelPrediction()

        return suggestionsEngine.generateSuggestions(allTrips, prediction, upcomingTrips)
    }

    fun getPredictionFlow(): Flow<TravelPrediction> {
        // Converti LiveData in Flow e poi mappa le predizioni
        return tripDao.getAll().asFlow().map { trips ->
            predictionAlgorithm.calculatePrediction(trips)
        }
    }

    // Metodi helper per ottenere dati sincroni
    private suspend fun getAllTripsSync(): List<TripEntity> {
        // Se hai una versione sincrona nel DAO, usala
        // Altrimenti, dovrai creare una query sincrona nel DAO
        return try {
            tripDao.getAllTripsSync() // Questo metodo deve essere aggiunto al DAO
        } catch (e: Exception) {
            // Fallback: restituisci lista vuota se il metodo non esiste
            emptyList()
        }
    }

    private suspend fun getTripsByStatusSync(status: TripStatus): List<TripEntity> {
        return try {
            tripDao.getTripsByStatusSync(status) // Questo metodo deve essere aggiunto al DAO
        } catch (e: Exception) {
            emptyList()
        }
    }
}