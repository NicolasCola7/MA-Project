package com.example.travel_companion.domain.model

data class TravelPrediction(
    val predictedDistance: Double, // km nel prossimo mese
    val predictedTripsCount: Int, // numero viaggi nel prossimo mese
    val confidence: Float, // 0.0 - 1.0
    val trend: TravelTrend,
    val calculatedAt: Long = System.currentTimeMillis()
)

enum class TravelTrend {
    INCREASING,
    STABLE,
    DECREASING,
    INSUFFICIENT_DATA
}