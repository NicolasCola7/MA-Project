package com.example.travel_companion.domain.model

data class TripSuggestion(
    val id: String,
    val title: String,
    val description: String,
    val destination: String,
    val type: String,
    val priority: SuggestionPriority,
    val reason: String,
)

enum class SuggestionPriority {
    HIGH, MEDIUM
}