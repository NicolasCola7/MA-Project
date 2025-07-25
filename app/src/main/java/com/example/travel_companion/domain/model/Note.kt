package com.example.travel_companion.domain.model

data class Note (
    val id: Long,
    val tripId: Long,
    val title: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
){}