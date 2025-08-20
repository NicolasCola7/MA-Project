package com.example.travel_companion.util

import com.example.travel_companion.data.local.entity.TripEntity
import com.example.travel_companion.domain.model.TravelPrediction
import com.example.travel_companion.domain.model.TravelTrend
import com.example.travel_companion.domain.model.TripStatus
import java.util.*
import kotlin.math.*

class TravelPredictionAlgorithm {

    companion object {
        private const val MIN_TRIPS_FOR_PREDICTION = 3
        private const val MONTHS_TO_ANALYZE = 6 // Analizza ultimi 6 mesi
        private const val TREND_THRESHOLD = 0.15f // 15% variazione per determinare trend
    }

    fun calculatePrediction(trips: List<TripEntity>): TravelPrediction {
        val completedTrips = trips.filter { it.status == TripStatus.FINISHED }

        if (completedTrips.size < MIN_TRIPS_FOR_PREDICTION) {
            return TravelPrediction(
                predictedDistance = 0.0,
                predictedTripsCount = 0,
                confidence = 0.0f,
                trend = TravelTrend.INSUFFICIENT_DATA
            )
        }

        val monthlyStats = calculateMonthlyStatistics(completedTrips)
        val trend = calculateTrend(monthlyStats)
        val prediction = generatePrediction(monthlyStats, trend)

        return TravelPrediction(
            predictedDistance = prediction.first,
            predictedTripsCount = prediction.second,
            confidence = calculateConfidence(monthlyStats, completedTrips.size),
            trend = trend
        )
    }

    private data class MonthlyStats(
        val year: Int,
        val month: Int,
        val tripsCount: Int,
        val totalDistance: Double,
        val timestamp: Long
    )

    private fun calculateMonthlyStatistics(trips: List<TripEntity>): List<MonthlyStats> {
        val calendar = Calendar.getInstance()
        val cutoffTime = calendar.timeInMillis - (MONTHS_TO_ANALYZE * 30L * 24 * 60 * 60 * 1000)

        val recentTrips = trips.filter { it.startDate >= cutoffTime }

        return recentTrips
            .groupBy { trip ->
                calendar.timeInMillis = trip.startDate
                Pair(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH))
            }
            .map { (yearMonth, monthTrips) ->
                calendar.set(yearMonth.first, yearMonth.second, 1)
                MonthlyStats(
                    year = yearMonth.first,
                    month = yearMonth.second,
                    tripsCount = monthTrips.size,
                    totalDistance = monthTrips.sumOf { it.trackedDistance / 1000.0 }, // Convert to km
                    timestamp = calendar.timeInMillis
                )
            }
            .sortedBy { it.timestamp }
    }

    private fun calculateTrend(monthlyStats: List<MonthlyStats>): TravelTrend {
        if (monthlyStats.size < 2) return TravelTrend.INSUFFICIENT_DATA

        val recentMonths = monthlyStats.takeLast(3)
        val olderMonths = monthlyStats.dropLast(3).takeLast(3)

        if (olderMonths.isEmpty()) return TravelTrend.INSUFFICIENT_DATA

        val recentAvgTrips = recentMonths.map { it.tripsCount }.average()
        val olderAvgTrips = olderMonths.map { it.tripsCount }.average()

        val changePercent = (recentAvgTrips - olderAvgTrips) / olderAvgTrips

        return when {
            changePercent > TREND_THRESHOLD -> TravelTrend.INCREASING
            changePercent < -TREND_THRESHOLD -> TravelTrend.DECREASING
            else -> TravelTrend.STABLE
        }
    }

    private fun generatePrediction(
        monthlyStats: List<MonthlyStats>,
        trend: TravelTrend
    ): Pair<Double, Int> {
        if (monthlyStats.isEmpty()) return Pair(0.0, 0)

        // Calcola medie ponderate (mesi più recenti hanno peso maggiore)
        val weights = monthlyStats.indices.map { i ->
            1.0 + (i.toDouble() / monthlyStats.size)
        }

        val weightedAvgTrips = monthlyStats.mapIndexed { i, stats ->
            stats.tripsCount * weights[i]
        }.sum() / weights.sum()

        val weightedAvgDistance = monthlyStats.mapIndexed { i, stats ->
            stats.totalDistance * weights[i]
        }.sum() / weights.sum()

        // Applica fattore di trend
        val trendFactor = when (trend) {
            TravelTrend.INCREASING -> 1.2
            TravelTrend.DECREASING -> 0.8
            TravelTrend.STABLE -> 1.0
            TravelTrend.INSUFFICIENT_DATA -> 1.0
        }

        // Applica fattore stagionale (esempio: estate = più viaggi)
        val seasonalFactor = getSeasonalFactor()

        val predictedTrips = (weightedAvgTrips * trendFactor * seasonalFactor)
            .roundToInt()
            .coerceAtLeast(0)

        val predictedDistance = (weightedAvgDistance * trendFactor * seasonalFactor)
            .coerceAtLeast(0.0)

        return Pair(predictedDistance, predictedTrips)
    }

    private fun getSeasonalFactor(): Double {
        val month = Calendar.getInstance().get(Calendar.MONTH)
        return when (month) {
            Calendar.JUNE, Calendar.JULY, Calendar.AUGUST -> 1.3 // Estate
            Calendar.DECEMBER, Calendar.JANUARY, Calendar.FEBRUARY -> 0.8 // Inverno
            Calendar.MARCH, Calendar.APRIL, Calendar.MAY,
            Calendar.SEPTEMBER, Calendar.OCTOBER -> 1.1 // Primavera/Autunno
            else -> 1.0
        }
    }

    private fun calculateConfidence(
        monthlyStats: List<MonthlyStats>,
        totalTrips: Int
    ): Float {
        // Fattori che influenzano la confidenza:
        // 1. Numero di viaggi storici
        val dataPointsFactor = min(totalTrips.toFloat() / 20f, 1.0f)

        // 2. Consistenza dei dati (varianza)
        val consistency = if (monthlyStats.isNotEmpty()) {
            val tripsVariance = calculateVariance(monthlyStats.map { it.tripsCount.toDouble() })
            val avgTrips = monthlyStats.map { it.tripsCount }.average()
            val coefficientOfVariation = if (avgTrips > 0) sqrt(tripsVariance) / avgTrips else 1.0
            (1.0 - min(coefficientOfVariation, 1.0)).toFloat()
        } else 0f

        // 3. Recency - dati più recenti = maggiore confidenza
        val recencyFactor = if (monthlyStats.isNotEmpty()) {
            val lastTripTime = monthlyStats.maxOf { it.timestamp }
            val daysSinceLastTrip = (System.currentTimeMillis() - lastTripTime) / (24 * 60 * 60 * 1000)
            max(0f, 1f - (daysSinceLastTrip.toFloat() / 90f)) // Diminuisce dopo 90 giorni
        } else 0f

        return ((dataPointsFactor + consistency + recencyFactor) / 3f)
            .coerceIn(0f, 1f)
    }

    private fun calculateVariance(values: List<Double>): Double {
        if (values.isEmpty()) return 0.0
        val mean = values.average()
        return values.map { (it - mean).pow(2) }.average()
    }
}