package com.example.travel_companion.domain.model

enum class TripStatus(private val value: String) {
    PLANNED("Pianificato"),
    STARTED("In corso"),
    PAUSED("In pausa"),
    FINISHED("Terminato");

    fun getValue(): String = value
}