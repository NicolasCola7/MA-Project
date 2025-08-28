package com.example.travel_companion.domain.model

data class TripPrediction(
    val predictedDistance: Double, // km in the next month
    val predictedTripsCount: Int, // number of trips in the next month
    val confidence: Float, // 0.0 - 1.0
    val trend: TripTrend,
    val calculatedAt: Long = System.currentTimeMillis()
)

enum class TripTrend {
    INCREASING,
    STABLE,
    DECREASING,
    INSUFFICIENT_DATA
}