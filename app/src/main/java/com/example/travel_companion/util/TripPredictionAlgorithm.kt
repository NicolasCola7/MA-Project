package com.example.travel_companion.util

import com.example.travel_companion.data.local.entity.TripEntity
import com.example.travel_companion.domain.model.TravelPrediction
import com.example.travel_companion.domain.model.TravelTrend
import com.example.travel_companion.domain.model.TripStatus
import java.util.*
import kotlin.math.min

class TripPredictionAlgorithm {

    companion object {
        private const val MIN_TRIPS_FOR_PREDICTION = 3
        private const val MONTHS_TO_ANALYZE = 4
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

        val monthlyData = getRecentMonthlyData(completedTrips)
        val trend = calculateTrend(monthlyData)
        val avgTripsPerMonth = monthlyData.map { it.tripsCount }.average()
        val avgDistancePerMonth = monthlyData.map { it.totalDistance }.average()

        // Predizione semplice basata sulla media e trend
        val trendMultiplier = when (trend) {
            TravelTrend.INCREASING -> 1.2
            TravelTrend.DECREASING -> 0.8
            else -> 1.0
        }

        return TravelPrediction(
            predictedDistance = avgDistancePerMonth * trendMultiplier,
            predictedTripsCount = (avgTripsPerMonth * trendMultiplier).toInt(),
            confidence = calculateConfidence(completedTrips.size, monthlyData.size),
            trend = trend
        )
    }

    private data class MonthlyData(
        val tripsCount: Int,
        val totalDistance: Double
    )

    private fun getRecentMonthlyData(trips: List<TripEntity>): List<MonthlyData> {
        val calendar = Calendar.getInstance()
        val cutoffTime = calendar.timeInMillis - (MONTHS_TO_ANALYZE * 30L * 24 * 60 * 60 * 1000)

        return trips
            .filter { it.startDate >= cutoffTime }
            .groupBy { trip ->
                calendar.timeInMillis = trip.startDate
                "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.MONTH)}"
            }
            .map { (_, monthTrips) ->
                MonthlyData(
                    tripsCount = monthTrips.size,
                    totalDistance = monthTrips.sumOf { it.trackedDistance / 1000.0 }
                )
            }
    }

    private fun calculateTrend(monthlyData: List<MonthlyData>): TravelTrend {
        if (monthlyData.size < 2) return TravelTrend.INSUFFICIENT_DATA

        val recentTrips = monthlyData.takeLast(2).sumOf { it.tripsCount }
        val olderTrips = monthlyData.dropLast(2).sumOf { it.tripsCount }

        return when {
            recentTrips > olderTrips -> TravelTrend.INCREASING
            recentTrips < olderTrips -> TravelTrend.DECREASING
            else -> TravelTrend.STABLE
        }
    }

    private fun calculateConfidence(totalTrips: Int, monthsWithData: Int): Float {
        val dataFactor = min(totalTrips.toFloat() / 10f, 1.0f)
        val consistencyFactor = min(monthsWithData.toFloat() / 3f, 1.0f)
        return (dataFactor + consistencyFactor) / 2f
    }
}