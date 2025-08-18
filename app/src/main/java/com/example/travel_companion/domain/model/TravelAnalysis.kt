package com.example.travel_companion.domain.model

data class TravelAnalysis(
    val totalTrips: Int,
    val averageTripsPerMonth: Double,
    val favoriteDestinationType: String,
    val averageTripDuration: Double,
    val mostActiveMonth: Int,
    val averageDistancePerTrip: Double,
    val nextPredictedTripDate: Long,
    val tripPredictions: List<TripPrediction>,
    val poiSuggestions: List<POISuggestion>
)