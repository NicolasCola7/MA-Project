package com.example.travel_companion.util.trip

import com.example.travel_companion.data.local.entity.TripEntity
import com.example.travel_companion.domain.model.*
import java.util.*

class TripSuggestionsEngine {

    // Pool completo di destinazioni
    private val suggestionTemplates = listOf(
        // Esperienze culturali
        SuggestionTemplate("Viaggio culturale", "Esplora musei e monumenti", "Cultura", "Immergiti nella storia e nell'arte"),
        SuggestionTemplate("Tour dei centri storici", "Passeggia tra vicoli antichi", "Cultura", "Scopri l'architettura medievale"),
        SuggestionTemplate("Weekend artistico", "Visita gallerie e mostre", "Cultura", "Arte contemporanea e classica"),

        // Esperienze naturalistiche
        SuggestionTemplate("Trekking in montagna", "Sentieri panoramici", "Natura", "Aria pura e paesaggi mozzafiato"),
        SuggestionTemplate("Escursione nei parchi", "Natura incontaminata", "Natura", "Flora e fauna selvatica"),
        SuggestionTemplate("Camminate nei boschi", "Percorsi immersi nel verde", "Natura", "Relax nella natura"),

        // Esperienze marine
        SuggestionTemplate("Vacanza al mare", "Relax in spiaggia", "Mare","Sole, sabbia e onde cristalline"),
        SuggestionTemplate("Tour costiero", "Scopri borghi marinari", "Mare", "Tradizioni marinare e panorami"),
        SuggestionTemplate("Weekend sul lungomare", "Passeggiate vista oceano", "Mare", "Tramonti indimenticabili"),

        // Esperienze gastronomiche
        SuggestionTemplate("Tour enogastronomico", "Sapori locali autentici", "Gastronomia", "Cucina tradizionale e vini"),
        SuggestionTemplate("Mercati e sagre", "Prodotti tipici regionali", "Gastronomia", "Tradizioni culinarie locali"),
        SuggestionTemplate("Cooking class", "Impara ricette locali", "Gastronomia", "Corso di cucina tradizionale"),

        // Esperienze romantiche
        SuggestionTemplate("Fuga romantica", "Momenti a due speciali", "Romantico", "Atmosfere suggestive e intime"),
        SuggestionTemplate("Cena con vista", "Ristoranti panoramici", "Romantico", "Cene romantiche indimenticabili"),
        SuggestionTemplate("Weekend spa", "Relax di coppia", "Romantico", "Benessere e intimità"),

        // Esperienze business/urban
        SuggestionTemplate("City break", "Scopri metropoli dinamiche", "Business", "Grattacieli e vita urbana"),
        SuggestionTemplate("Shopping tour", "Quartieri della moda", "Business", "Negozi e design contemporaneo"),
        SuggestionTemplate("Eventi e fiere", "Networking e cultura", "Business", "Opportunità professionali"),

        // Esperienze relax
        SuggestionTemplate("Ritiro benessere", "Pace e tranquillità", "Relax", "Terme e centri benessere"),
        SuggestionTemplate("Vacanza slow", "Ritmi rilassati", "Relax", "Disconnetti e ricaricati"),
        SuggestionTemplate("Meditazione in natura", "Mindfulness all'aperto", "Relax", "Equilibrio interiore"),

        // Esperienze avventura
        SuggestionTemplate("Weekend avventura", "Attività adrenaliniche", "Avventura", "Sport estremi e sfide"),
        SuggestionTemplate("Esplorazione outdoor", "Attività all'aria aperta", "Avventura", "Kayak, arrampicata, cycling"),
        SuggestionTemplate("Safari urbano", "Scopri angoli nascosti", "Avventura", "Esplora luoghi insoliti")
    )

    private data class SuggestionTemplate(
        val destination: String,
        val title: String,
        val type: String,
        val description: String
    )

    fun generateSuggestions(
        trips: List<TripEntity>,
        prediction: TripPrediction,
        upcomingTrips: List<TripEntity>
    ): List<TripSuggestion> {
        val suggestions = mutableListOf<TripSuggestion>()
        val userPreferences = analyzeUserPreferences(trips)

        // 1. Suggerimenti motivazionali se pochi viaggi programmati
        if (upcomingTrips.isEmpty() || prediction.trend == TripTrend.DECREASING) {
            suggestions.addAll(generateMotivationalSuggestions())
        }

        // 2. Suggerimenti basati su preferenze utente
        suggestions.addAll(generatePersonalizedSuggestions(userPreferences))

        // 3. Suggerimenti stagionali
        suggestions.addAll(generateSeasonalSuggestions())

        return suggestions
            .distinctBy { it.title }
            .take(8) // Massimo 8 suggerimenti con pool più grande
    }

    private fun analyzeUserPreferences(trips: List<TripEntity>): UserPreferences {
        val completedTrips = trips.filter { it.status == TripStatus.FINISHED }

        if (completedTrips.isEmpty()) {
            return UserPreferences(
                favoriteTypes = listOf("Cultura", "Natura", "Mare"),
                visitedDestinations = emptySet()
            )
        }

        val typeFrequency = completedTrips.groupingBy { it.type }.eachCount()
        val favoriteTypes = typeFrequency.entries
            .sortedByDescending { it.value }
            .take(2)
            .map { it.key }

        return UserPreferences(
            favoriteTypes = favoriteTypes,
            visitedDestinations = completedTrips.map { it.destination }.toSet()
        )
    }

    private data class UserPreferences(
        val favoriteTypes: List<String>,
        val visitedDestinations: Set<String>
    )

    private fun generateMotivationalSuggestions(): List<TripSuggestion> {
        return suggestionTemplates
            .take(2)
            .mapIndexed { index, template ->
                TripSuggestion(
                    id = "motivational_$index",
                    title = template.title,
                    description = template.description,
                    destination = template.destination,
                    type = template.type,
                    priority = SuggestionPriority.HIGH,
                    reason = "È il momento perfetto per una nuova esperienza!"
                )
            }
    }

    private fun generatePersonalizedSuggestions(preferences: UserPreferences): List<TripSuggestion> {
        return suggestionTemplates
            .filter { preferences.favoriteTypes.contains(it.type) }
            .take(2)
            .mapIndexed { index, template ->
                TripSuggestion(
                    id = "personalized_$index",
                    title = template.title,
                    description = template.description,
                    destination = template.destination,
                    type = template.type,
                    priority = SuggestionPriority.MEDIUM,
                    reason = "Basato sui tuoi interessi per ${template.type.lowercase()}"
                )
            }
    }

    private fun generateSeasonalSuggestions(): List<TripSuggestion> {
        val currentSeason = getCurrentSeason()
        val seasonalTypes = when (currentSeason) {
            "Inverno" -> listOf("Relax", "Cultura", "Business")
            "Primavera" -> listOf("Natura", "Avventura", "Gastronomia")
            "Estate" -> listOf("Mare", "Avventura", "Natura")
            "Autunno" -> listOf("Gastronomia", "Romantico", "Cultura")
            else -> listOf("Cultura")
        }

        return suggestionTemplates
            .filter { seasonalTypes.contains(it.type) }
            .take(2)
            .mapIndexed { index, template ->
                TripSuggestion(
                    id = "seasonal_$index",
                    title = template.title,
                    description = template.description,
                    destination = template.destination,
                    type = template.type,
                    priority = SuggestionPriority.MEDIUM,
                    reason = "Ideale per $currentSeason"
                )
            }
    }

    private fun getCurrentSeason(): String {
        return when (Calendar.getInstance().get(Calendar.MONTH)) {
            Calendar.DECEMBER, Calendar.JANUARY, Calendar.FEBRUARY -> "Inverno"
            Calendar.MARCH, Calendar.APRIL, Calendar.MAY -> "Primavera"
            Calendar.JUNE, Calendar.JULY, Calendar.AUGUST -> "Estate"
            else -> "Autunno"
        }
    }
}