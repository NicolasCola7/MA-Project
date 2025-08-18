package com.example.travel_companion.domain.model

data class POISuggestion(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val category: String,
    val confidence: Double,
    val reasoning: String,
    val estimatedDistance: Double
)