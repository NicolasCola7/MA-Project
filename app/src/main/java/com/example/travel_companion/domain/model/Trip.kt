package com.example.travel_companion.domain.model

data class Trip(
    val id: Long,
    val destination: String,
    val startDate: Long,
    val endDate: Long?,
    val type: String,
    val status: TripStatus = TripStatus.PLANNED
) {
}