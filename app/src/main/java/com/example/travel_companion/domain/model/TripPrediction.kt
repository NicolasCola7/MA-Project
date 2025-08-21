package com.example.travel_companion.domain.model

data class TripPrediction(
    val predictedDistance: Double, // km nel prossimo mese
    val predictedTripsCount: Int, // numero viaggi nel prossimo mese
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