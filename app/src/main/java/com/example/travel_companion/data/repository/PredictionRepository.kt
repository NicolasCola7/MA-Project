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

    suspend fun getPredictionInsights(): List<PredictionInsight> {
        val prediction = calculateTravelPrediction()
        val allTrips = getAllTripsSync()
        val upcomingTrips = getTripsByStatusSync(TripStatus.PLANNED)

        return generateInsights(prediction, allTrips, upcomingTrips)
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

    private fun generateInsights(
        prediction: TravelPrediction,
        allTrips: List<TripEntity>,
        upcomingTrips: List<TripEntity>
    ): List<PredictionInsight> {
        val insights = mutableListOf<PredictionInsight>()

        // Insight su trend
        when (prediction.trend) {
            TravelTrend.INCREASING -> {
                insights.add(
                    PredictionInsight(
                        type = InsightType.ACHIEVEMENT,
                        message = "I tuoi viaggi sono in crescita! Continua così!",
                        actionText = "Vedi statistiche",
                        actionType = ActionType.VIEW_STATISTICS
                    )
                )
            }
            TravelTrend.DECREASING -> {
                insights.add(
                    PredictionInsight(
                        type = InsightType.WARNING,
                        message = "I tuoi viaggi stanno diminuendo. Che ne dici di pianificare qualcosa?",
                        actionText = "Scopri suggerimenti",
                        actionType = ActionType.VIEW_SUGGESTIONS
                    )
                )
            }
            TravelTrend.STABLE -> {
                insights.add(
                    PredictionInsight(
                        type = InsightType.INFO,
                        message = "Mantieni un ritmo costante nei viaggi. Ottimo equilibrio!",
                        actionText = "Pianifica viaggio",
                        actionType = ActionType.PLAN_TRIP
                    )
                )
            }
            TravelTrend.INSUFFICIENT_DATA -> {
                insights.add(
                    PredictionInsight(
                        type = InsightType.SUGGESTION,
                        message = "Inizia a tracciare i tuoi viaggi per ottenere previsioni personalizzate!",
                        actionText = "Pianifica primo viaggio",
                        actionType = ActionType.PLAN_TRIP
                    )
                )
            }
        }

        // Insight su previsioni
        if (prediction.confidence > 0.7f) {
            if (prediction.predictedTripsCount > 2) {
                insights.add(
                    PredictionInsight(
                        type = InsightType.ACHIEVEMENT,
                        message = "Prevediamo ${prediction.predictedTripsCount} viaggi per il prossimo mese!",
                        actionText = "Pianifica ora",
                        actionType = ActionType.PLAN_TRIP
                    )
                )
            } else if (prediction.predictedTripsCount == 0 && upcomingTrips.isEmpty()) {
                insights.add(
                    PredictionInsight(
                        type = InsightType.SUGGESTION,
                        message = "Il prossimo mese sembra libero. Perfetto per un'avventura!",
                        actionText = "Esplora destinazioni",
                        actionType = ActionType.VIEW_SUGGESTIONS
                    )
                )
            }
        }

        // Insight su distanza
        if (prediction.predictedDistance > 100.0) {
            insights.add(
                PredictionInsight(
                    type = InsightType.INFO,
                    message = "Previsione: ${String.format("%.1f", prediction.predictedDistance)} km il prossimo mese"
                )
            )
        }

        // Insight basati su stagione
        val currentMonth = java.util.Calendar.getInstance().get(java.util.Calendar.MONTH)
        if (currentMonth in 5..7 && upcomingTrips.isEmpty()) { // Estate (Giugno=5, Luglio=6, Agosto=7)
            insights.add(
                PredictionInsight(
                    type = InsightType.SUGGESTION,
                    message = "È estate! Il momento perfetto per esplorare la costa italiana",
                    actionText = "Vedi destinazioni estive",
                    actionType = ActionType.VIEW_SUGGESTIONS
                )
            )
        }

        return insights
    }
}