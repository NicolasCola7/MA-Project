package com.example.travel_companion.util.trip

import com.example.travel_companion.data.local.entity.TripEntity
import com.example.travel_companion.domain.model.TripPrediction
import com.example.travel_companion.domain.model.TripTrend
import com.example.travel_companion.domain.model.TripStatus
import java.util.*
import kotlin.math.min

/**
 * Provides a prediction of future trips based on past trip data.
 * Analyzes completed trips over recent months to determine trends,
 * average distance, number of trips, and confidence in the prediction.
 */
class TripPredictionAlgorithm {

    companion object {
        private const val MIN_TRIPS_FOR_PREDICTION = 3
        private const val MONTHS_TO_ANALYZE = 4
    }

    /**
     * Calculates a trip prediction based on a list of completed trips.
     * Returns predicted distance, number of trips, trend, and confidence level.
     *
     * @param trips List of TripEntity objects representing past trips.
     * @return TripPrediction object containing prediction metrics.
     */
    fun calculatePrediction(trips: List<TripEntity>): TripPrediction {
        val completedTrips = trips.filter { it.status == TripStatus.FINISHED }

        if (completedTrips.size < MIN_TRIPS_FOR_PREDICTION) {
            return TripPrediction(
                predictedDistance = 0.0,
                predictedTripsCount = 0,
                confidence = 0.0f,
                trend = TripTrend.INSUFFICIENT_DATA
            )
        }

        val monthlyData = getRecentMonthlyData(completedTrips)
        val trend = calculateTrend(monthlyData)
        val avgTripsPerMonth = monthlyData.map { it.tripsCount }.average()
        val avgDistancePerMonth = monthlyData.map { it.totalDistance }.average()

        // Adjust prediction based on detected trend
        val trendMultiplier = when (trend) {
            TripTrend.INCREASING -> 1.2
            TripTrend.DECREASING -> 0.8
            else -> 1.0
        }

        return TripPrediction(
            predictedDistance = avgDistancePerMonth * trendMultiplier,
            predictedTripsCount = (avgTripsPerMonth * trendMultiplier).toInt(),
            confidence = calculateConfidence(completedTrips.size, monthlyData.size),
            trend = trend
        )
    }

    /**
     * Represents aggregated trip data for a single month.
     *
     * @property tripsCount Number of trips in the month.
     * @property totalDistance Total distance traveled in the month (km).
     */
    private data class MonthlyData(
        val tripsCount: Int,
        val totalDistance: Double
    )

    /**
     * Extracts trip data for the most recent months.
     *
     * @param trips List of completed TripEntity objects.
     * @return List of MonthlyData representing aggregated trips per month.
     */
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

    /**
     * Determines the trend of trips over recent months.
     *
     * @param monthlyData List of MonthlyData for the analyzed months.
     * @return TripTrend indicating if trip frequency is increasing, decreasing, stable, or insufficient.
     */
    private fun calculateTrend(monthlyData: List<MonthlyData>): TripTrend {
        if (monthlyData.size < 2) return TripTrend.INSUFFICIENT_DATA

        val recentTrips = monthlyData.takeLast(2).sumOf { it.tripsCount }
        val olderTrips = monthlyData.dropLast(2).sumOf { it.tripsCount }

        return when {
            recentTrips > olderTrips -> TripTrend.INCREASING
            recentTrips < olderTrips -> TripTrend.DECREASING
            else -> TripTrend.STABLE
        }
    }

    /**
     * Calculates a confidence score for the prediction.
     * Confidence is based on the number of trips and consistency across months.
     *
     * @param totalTrips Total number of completed trips analyzed.
     * @param monthsWithData Number of months with available trip data.
     * @return Float confidence score between 0.0 and 1.0.
     */
    private fun calculateConfidence(totalTrips: Int, monthsWithData: Int): Float {
        val dataFactor = min(totalTrips.toFloat() / 10f, 1.0f)
        val consistencyFactor = min(monthsWithData.toFloat() / 3f, 1.0f)
        return (dataFactor + consistencyFactor) / 2f
    }
}
