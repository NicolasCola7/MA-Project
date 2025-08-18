package com.example.travel_companion.domain.model

data class TripPrediction(
    val suggestedDestination: String,
    val predictedType: String,
    val suggestedStartDate: Long,
    val suggestedEndDate: Long,
    val confidence: Double,
    val reasoning: String,
    val suggestedLatitude: Double,
    val suggestedLongitude: Double
)