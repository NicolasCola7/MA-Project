package com.example.travel_companion.domain.model

data class Coordinate(
    val id: Long,
    val tripId: Long,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val altitude: Double?,
    val accuracy: Float?,
    val speed: Float?,
) {}