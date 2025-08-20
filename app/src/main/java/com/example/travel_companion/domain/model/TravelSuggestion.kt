package com.example.travel_companion.domain.model

data class TravelSuggestion(
    val id: String,
    val title: String,
    val description: String,
    val destination: String,
    val estimatedDistance: Double,
    val type: String,
    val priority: SuggestionPriority,
    val reason: String, // Motivo del suggerimento
    val imageUrl: String? = null
)

enum class SuggestionPriority {
    HIGH, MEDIUM, LOW
}