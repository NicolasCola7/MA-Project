package com.example.travel_companion.domain.model

data class Photo(
    val id: Long,
    val tripId: Long,
    val uri: String,
    val timestamp: Long = System.currentTimeMillis()
) {
}