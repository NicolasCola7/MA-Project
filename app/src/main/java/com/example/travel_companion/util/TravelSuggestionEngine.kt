package com.example.travel_companion.util

import com.example.travel_companion.data.local.entity.TripEntity
import com.example.travel_companion.domain.model.*
import java.util.*

class TravelSuggestionsEngine {

    private val destinationPool = listOf(
        // Esperienze culturali
        SuggestionTemplate("Viaggio culturale", "Esplora musei e monumenti", "Cultura", 45.0, "Immergiti nella storia e nell'arte"),
        SuggestionTemplate("Tour dei centri storici", "Passeggia tra vicoli antichi", "Cultura", 35.0, "Scopri l'architettura medievale"),
        SuggestionTemplate("Weekend artistico", "Visita gallerie e mostre", "Cultura", 40.0, "Arte contemporanea e classica"),

        // Esperienze naturalistiche
        SuggestionTemplate("Trekking in montagna", "Sentieri panoramici", "Natura", 80.0, "Aria pura e paesaggi mozzafiato"),
        SuggestionTemplate("Escursione nei parchi", "Natura incontaminata", "Natura", 60.0, "Flora e fauna selvatica"),
        SuggestionTemplate("Camminate nei boschi", "Percorsi immersi nel verde", "Natura", 50.0, "Relax nella natura"),

        // Esperienze marine
        SuggestionTemplate("Vacanza al mare", "Relax in spiaggia", "Mare", 70.0, "Sole, sabbia e onde cristalline"),
        SuggestionTemplate("Tour costiero", "Scopri borghi marinari", "Mare", 85.0, "Tradizioni marinare e panorami"),
        SuggestionTemplate("Weekend sul lungomare", "Passeggiate vista oceano", "Mare", 65.0, "Tramonti indimenticabili"),

        // Esperienze gastronomiche
        SuggestionTemplate("Tour enogastronomico", "Sapori locali autentici", "Gastronomia", 40.0, "Cucina tradizionale e vini"),
        SuggestionTemplate("Mercati e sagre", "Prodotti tipici regionali", "Gastronomia", 30.0, "Tradizioni culinarie locali"),
        SuggestionTemplate("Cooking class", "Impara ricette locali", "Gastronomia", 25.0, "Corso di cucina tradizionale"),

        // Esperienze romantiche
        SuggestionTemplate("Fuga romantica", "Momenti a due speciali", "Romantico", 55.0, "Atmosfere suggestive e intime"),
        SuggestionTemplate("Cena con vista", "Ristoranti panoramici", "Romantico", 45.0, "Cene romantiche indimenticabili"),
        SuggestionTemplate("Weekend spa", "Relax di coppia", "Romantico", 60.0, "Benessere e intimità"),

        // Esperienze business/urban
        SuggestionTemplate("City break", "Scopri metropoli dinamiche", "Business", 40.0, "Grattacieli e vita urbana"),
        SuggestionTemplate("Shopping tour", "Quartieri della moda", "Business", 35.0, "Negozi e design contemporaneo"),
        SuggestionTemplate("Eventi e fiere", "Networking e cultura", "Business", 50.0, "Opportunità professionali"),

        // Esperienze relax
        SuggestionTemplate("Ritiro benessere", "Pace e tranquillità", "Relax", 65.0, "Terme e centri benessere"),
        SuggestionTemplate("Vacanza slow", "Ritmi rilassati", "Relax", 55.0, "Disconnetti e ricaricati"),
        SuggestionTemplate("Meditazione in natura", "Mindfulness all'aperto", "Relax", 45.0, "Equilibrio interiore"),

        // Esperienze avventura
        SuggestionTemplate("Weekend avventura", "Attività adrenaliniche", "Avventura", 75.0, "Sport estremi e sfide"),
        SuggestionTemplate("Esplorazione outdoor", "Attività all'aria aperta", "Avventura", 85.0, "Kayak, arrampicata, cycling"),
        SuggestionTemplate("Safari urbano", "Scopri angoli nascosti", "Avventura", 30.0, "Esplora luoghi insoliti")
    )

    private data class SuggestionTemplate(
        val destination: String,
        val title: String,
        val type: String,
        val estimatedDistance: Double,
        val description: String
    )

    // Definisci le costanti per le stagioni
    companion object {
        private const val WINTER = 0
        private const val SPRING = 1
        private const val SUMMER = 2
        private const val FALL = 3
    }

    fun generateSuggestions(
        trips: List<TripEntity>,
        prediction: TravelPrediction,
        upcomingTrips: List<TripEntity>
    ): List<TravelSuggestion> {
        val suggestions = mutableListOf<TravelSuggestion>()

        // Analizza pattern utente
        val userProfile = analyzeUserProfile(trips)

        // 1. Se trend in diminuzione o nessun viaggio programmato
        if (shouldSuggestMoreTravel(prediction, upcomingTrips)) {
            suggestions.addAll(generateMotivationalSuggestions(userProfile))
        }

        // 2. Suggerimenti basati su preferenze
        suggestions.addAll(generatePersonalizedSuggestions(userProfile, trips))

        // 3. Suggerimenti stagionali
        suggestions.addAll(generateSeasonalSuggestions())

        // 4. Suggerimenti per completare obiettivi
        suggestions.addAll(generateGoalBasedSuggestions(prediction, userProfile))

        return suggestions
            .distinctBy { it.destination }
            .sortedBy { it.priority }
            .take(8) // Massimo 8 suggerimenti
    }

    private data class UserProfile(
        val preferredTypes: List<String>,
        val averageDistance: Double,
        val preferredSeasons: List<Int>,
        val visitedDestinations: Set<String>,
        val activityLevel: ActivityLevel
    )

    private enum class ActivityLevel { LOW, MEDIUM, HIGH }

    private fun analyzeUserProfile(trips: List<TripEntity>): UserProfile {
        val completedTrips = trips.filter { it.status == TripStatus.FINISHED }

        if (completedTrips.isEmpty()) {
            return UserProfile(
                preferredTypes = listOf("Cultura", "Natura"),
                averageDistance = 50.0,
                preferredSeasons = listOf(SPRING, SUMMER),
                visitedDestinations = emptySet(),
                activityLevel = ActivityLevel.MEDIUM
            )
        }

        // Analizza tipi preferiti
        val typeFrequency = completedTrips.groupingBy { it.type }.eachCount()
        val preferredTypes = typeFrequency.entries
            .sortedByDescending { it.value }
            .take(3)
            .map { it.key }

        // Calcola distanza media
        val avgDistance = completedTrips.map { it.trackedDistance / 1000.0 }.average()

        // Analizza stagioni preferite
        val calendar = Calendar.getInstance()
        val seasonFrequency = completedTrips.map { trip ->
            calendar.timeInMillis = trip.startDate
            getSeason(calendar.get(Calendar.MONTH))
        }.groupingBy { it }.eachCount()

        val preferredSeasons = seasonFrequency.entries
            .sortedByDescending { it.value }
            .take(2)
            .map { it.key }

        // Determina livello di attività
        val tripsPerMonth = completedTrips.size.toDouble() / 12.0
        val activityLevel = when {
            tripsPerMonth >= 2.0 -> ActivityLevel.HIGH
            tripsPerMonth >= 0.5 -> ActivityLevel.MEDIUM
            else -> ActivityLevel.LOW
        }

        return UserProfile(
            preferredTypes = preferredTypes,
            averageDistance = avgDistance,
            preferredSeasons = preferredSeasons,
            visitedDestinations = completedTrips.map { it.destination }.toSet(),
            activityLevel = activityLevel
        )
    }

    private fun getSeason(month: Int): Int {
        return when (month) {
            Calendar.DECEMBER, Calendar.JANUARY, Calendar.FEBRUARY -> WINTER
            Calendar.MARCH, Calendar.APRIL, Calendar.MAY -> SPRING
            Calendar.JUNE, Calendar.JULY, Calendar.AUGUST -> SUMMER
            else -> FALL // Calendar.SEPTEMBER, Calendar.OCTOBER, Calendar.NOVEMBER
        }
    }

    private fun shouldSuggestMoreTravel(
        prediction: TravelPrediction,
        upcomingTrips: List<TripEntity>
    ): Boolean {
        return prediction.trend == TravelTrend.DECREASING ||
                upcomingTrips.isEmpty() ||
                prediction.predictedTripsCount < 1
    }

    private fun generateMotivationalSuggestions(userProfile: UserProfile): List<TravelSuggestion> {
        val messages = listOf(
            "Il momento perfetto per una nuova esperienza!",
            "Esplora qualcosa di diverso dal solito",
            "Una piccola avventura può riaccendere la passione",
            "Tempo di creare nuovi ricordi speciali!"
        )

        return destinationPool
            .filter { !userProfile.visitedDestinations.contains(it.destination) }
            .filter { it.estimatedDistance <= userProfile.averageDistance * 1.5 }
            .take(3)
            .mapIndexed { index, template ->
                TravelSuggestion(
                    id = "motivational_${template.destination.replace(" ", "_")}",
                    title = template.title,
                    description = template.description,
                    destination = template.destination,
                    estimatedDistance = template.estimatedDistance,
                    type = template.type,
                    priority = SuggestionPriority.HIGH,
                    reason = messages[index % messages.size]
                )
            }
    }

    private fun generatePersonalizedSuggestions(
        userProfile: UserProfile,
        trips: List<TripEntity>
    ): List<TravelSuggestion> {
        return destinationPool
            .filter { template ->
                // Filtra per tipo preferito
                userProfile.preferredTypes.contains(template.type) &&
                        // Non già visitato
                        !userProfile.visitedDestinations.contains(template.destination) &&
                        // Distanza appropriata
                        template.estimatedDistance <= userProfile.averageDistance * 2
            }
            .take(3)
            .map { template ->
                TravelSuggestion(
                    id = "personalized_${template.destination.replace(" ", "_")}",
                    title = template.title,
                    description = template.description,
                    destination = template.destination,
                    estimatedDistance = template.estimatedDistance,
                    type = template.type,
                    priority = SuggestionPriority.MEDIUM,
                    reason = "Basato sulle tue preferenze per ${template.type.lowercase()}"
                )
            }
    }

    private fun generateSeasonalSuggestions(): List<TravelSuggestion> {
        val currentMonth = Calendar.getInstance().get(Calendar.MONTH)
        val season = getSeason(currentMonth)

        val seasonalExperiences = when (season) {
            WINTER -> listOf("Ritiro benessere", "Tour dei centri storici", "Weekend artistico")
            SPRING -> listOf("Camminate nei boschi", "Weekend avventura", "Tour enogastronomico")
            SUMMER -> listOf("Vacanza al mare", "Tour costiero", "Escursione nei parchi")
            FALL -> listOf("Trekking in montagna", "Fuga romantica", "Meditazione in natura")
            else -> listOf("City break", "Viaggio culturale") // fallback
        }

        return destinationPool
            .filter { seasonalExperiences.contains(it.destination) }
            .take(2)
            .map { template ->
                TravelSuggestion(
                    id = "seasonal_${template.destination.replace(" ", "_")}",
                    title = template.title,
                    description = template.description,
                    destination = template.destination,
                    estimatedDistance = template.estimatedDistance,
                    type = template.type,
                    priority = SuggestionPriority.MEDIUM,
                    reason = "Ideale per questa stagione"
                )
            }
    }

    private fun generateGoalBasedSuggestions(
        prediction: TravelPrediction,
        userProfile: UserProfile
    ): List<TravelSuggestion> {
        val suggestions = mutableListOf<TravelSuggestion>()

        // Se la previsione è bassa, suggerisci esperienze brevi
        if (prediction.predictedTripsCount < 2) {
            val shortExperiences = destinationPool
                .filter { it.estimatedDistance <= 50.0 }
                .filter { !userProfile.visitedDestinations.contains(it.destination) }
                .take(1)

            shortExperiences.forEach { template ->
                suggestions.add(
                    TravelSuggestion(
                        id = "goal_${template.destination.replace(" ", "_")}",
                        title = "Weekend ${template.destination.lowercase()}",
                        description = template.description,
                        destination = template.destination,
                        estimatedDistance = template.estimatedDistance,
                        type = template.type,
                        priority = SuggestionPriority.LOW,
                        reason = "Perfetto per iniziare a viaggiare di più"
                    )
                )
            }
        }

        return suggestions
    }
}