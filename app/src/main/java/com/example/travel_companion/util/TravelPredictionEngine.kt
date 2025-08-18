package com.example.travel_companion.util

import android.util.Log
import com.example.travel_companion.data.local.entity.TripEntity
import com.example.travel_companion.data.local.entity.POIEntity
import com.example.travel_companion.domain.model.TripStatus
import com.example.travel_companion.domain.model.TravelAnalysis
import com.example.travel_companion.domain.model.TripPrediction
import com.example.travel_companion.domain.model.POISuggestion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.*
import kotlin.math.*

class TravelPredictionEngine {

    companion object {
        private const val TAG = "TravelPredictionEngine"
    }

    suspend fun analyzeAndPredict(
        trips: List<TripEntity>,
        pois: List<POIEntity>
    ): TravelAnalysis = withContext(Dispatchers.Default) {

        Timber.tag(TAG).d("=== INIZIO ANALISI PREDITTIVA ===")
        Timber.tag(TAG).d("Viaggi totali ricevuti: ${trips.size}")
        Timber.tag(TAG).d("POI totali ricevuti: ${pois.size}")

        val completedTrips = trips.filter { it.status == TripStatus.FINISHED }
        Timber.tag(TAG).d("Viaggi completati: ${completedTrips.size}")

        if (completedTrips.isEmpty()) {
            Timber.tag(TAG).w("Nessun viaggio completato trovato - ritorno analisi vuota")
            return@withContext createEmptyAnalysis()
        }

        // Log dettagli viaggi
        completedTrips.forEachIndexed { index, trip ->
            Timber.tag(TAG).d(
                "Viaggio $index: ${trip.destination}, tipo: ${trip.type}, " +
                        "coordinate: (${trip.destinationLatitude}, ${trip.destinationLongitude})"
            )
        }

        // Log dettagli POI
        pois.forEachIndexed { index, poi ->
            Timber.tag(TAG).d(
                "POI $index: ${poi.name}, " +
                        "coordinate: (${poi.latitude}, ${poi.longitude})"
            )
        }

        val statistics = calculateStatistics(completedTrips)
        Timber.tag(TAG).d("Statistiche calcolate:")
        Timber.tag(TAG).d("  - Tipo preferito: ${statistics.favoriteType}")
        Timber.tag(TAG).d("  - Viaggi/mese: ${statistics.avgTripsPerMonth}")
        Timber.tag(TAG).d("  - Cluster destinazioni: ${statistics.destinationClusters.size}")

        val tripPredictions = predictFutureTrips(completedTrips, statistics)
        Timber.tag(TAG).d("Previsioni viaggi generate: ${tripPredictions.size}")
        tripPredictions.forEachIndexed { index, prediction ->
            Timber.tag(TAG).d(
                "  Predizione $index: ${prediction.suggestedDestination} " +
                        "(confidenza: ${(prediction.confidence * 100).toInt()}%)"
            )
        }

        val poiSuggestions = suggestNewPOIs(completedTrips, pois)
        Timber.tag(TAG).d("Suggerimenti POI generati: ${poiSuggestions.size}")
        poiSuggestions.forEachIndexed { index, suggestion ->
            Timber.tag(TAG).d(
                "  Suggerimento $index: ${suggestion.name} - ${suggestion.category} " +
                        "(confidenza: ${(suggestion.confidence * 100).toInt()}%)"
            )
        }

        Timber.tag(TAG).d("=== FINE ANALISI PREDITTIVA ===")

        TravelAnalysis(
            totalTrips = completedTrips.size,
            averageTripsPerMonth = statistics.avgTripsPerMonth,
            favoriteDestinationType = statistics.favoriteType,
            averageTripDuration = statistics.avgDuration,
            mostActiveMonth = statistics.mostActiveMonth,
            averageDistancePerTrip = statistics.avgDistance,
            nextPredictedTripDate = statistics.nextPredictedDate,
            tripPredictions = tripPredictions,
            poiSuggestions = poiSuggestions
        )
    }

    // Aggiungi anche log nel metodo clusterDestinations:
    private fun clusterDestinations(trips: List<TripEntity>): List<DestinationCluster> {
        if (trips.isEmpty()) return emptyList()

        Timber.tag(TAG).d("Inizio clustering di ${trips.size} viaggi")

        val clusters = mutableListOf<DestinationCluster>()
        val processed = mutableSetOf<TripEntity>()

        trips.forEach { trip ->
            if (trip in processed) return@forEach

            val nearbyTrips = trips.filter { other ->
                other !in processed &&
                        calculateDistance(trip.destinationLatitude, trip.destinationLongitude,
                            other.destinationLatitude, other.destinationLongitude) <= 50.0
            }

            if (nearbyTrips.size >= 2) {
                val centerLat = nearbyTrips.map { it.destinationLatitude }.average()
                val centerLng = nearbyTrips.map { it.destinationLongitude }.average()
                val maxDistance = nearbyTrips.maxOf {
                    calculateDistance(centerLat, centerLng, it.destinationLatitude, it.destinationLongitude)
                }
                val dominantType = nearbyTrips.groupingBy { it.type }.eachCount().maxByOrNull { it.value }?.key ?: trip.type

                Timber.tag(TAG).d(
                    "Cluster trovato: ${nearbyTrips.size} viaggi, tipo dominante: $dominantType, " +
                            "centro: ($centerLat, $centerLng)"
                )

                clusters.add(DestinationCluster(centerLat, centerLng, maxDistance, nearbyTrips.size, dominantType))
                processed.addAll(nearbyTrips)
            }
        }

        Timber.tag(TAG).d("Clustering completato: ${clusters.size} cluster trovati")
        return clusters.sortedByDescending { it.tripCount }
    }

    private fun predictFutureTrips(trips: List<TripEntity>, stats: TravelStatistics): List<TripPrediction> {
        val predictions = mutableListOf<TripPrediction>()
        val calendar = Calendar.getInstance()

        // Predizione 1: Basata sul cluster di destinazioni più visitato
        if (stats.destinationClusters.isNotEmpty()) {
            val topCluster = stats.destinationClusters.first()
            val variation = 0.01 // ~1km di variazione

            predictions.add(TripPrediction(
                suggestedDestination = "Zona ${topCluster.dominantType} preferita",
                predictedType = topCluster.dominantType,
                suggestedStartDate = stats.nextPredictedDate,
                suggestedEndDate = stats.nextPredictedDate + (stats.avgDuration * 24 * 60 * 60 * 1000).toLong(),
                confidence = min(0.95, topCluster.tripCount * 0.15),
                reasoning = "Basato su ${topCluster.tripCount} viaggi precedenti in questa zona",
                suggestedLatitude = topCluster.centerLat + (Random().nextDouble() - 0.5) * variation,
                suggestedLongitude = topCluster.centerLng + (Random().nextDouble() - 0.5) * variation
            ))
        }

        // Predizione 2: Basata sul pattern stagionale
        calendar.timeInMillis = stats.nextPredictedDate
        val nextMonth = calendar.get(Calendar.MONTH)
        val seasonalConfidence = stats.monthlyPattern[nextMonth]?.toDouble()?.div(trips.size) ?: 0.1

        if (seasonalConfidence > 0.2) {
            val seasonalType = getSeasonalTripType(nextMonth)
            val seasonalDestination = generateSeasonalDestination(nextMonth, trips)

            predictions.add(TripPrediction(
                suggestedDestination = seasonalDestination.first,
                predictedType = seasonalType,
                suggestedStartDate = stats.nextPredictedDate + (7 * 24 * 60 * 60 * 1000), // +1 settimana
                suggestedEndDate = stats.nextPredictedDate + ((7 + stats.avgDuration) * 24 * 60 * 60 * 1000).toLong(),
                confidence = seasonalConfidence,
                reasoning = "Pattern stagionale: ${stats.monthlyPattern[nextMonth]} viaggi precedenti a ${getMonthName(nextMonth)}",
                suggestedLatitude = seasonalDestination.second,
                suggestedLongitude = seasonalDestination.third
            ))
        }

        // Predizione 3: Esplorazione di nuove aree
        val explorationDestination = generateExplorationDestination(trips, stats.destinationClusters)
        predictions.add(TripPrediction(
            suggestedDestination = explorationDestination.first,
            predictedType = stats.favoriteType,
            suggestedStartDate = stats.nextPredictedDate + (14 * 24 * 60 * 60 * 1000), // +2 settimane
            suggestedEndDate = stats.nextPredictedDate + ((14 + stats.avgDuration) * 24 * 60 * 60 * 1000).toLong(),
            confidence = 0.6,
            reasoning = "Suggerimento per esplorare nuove aree non ancora visitate",
            suggestedLatitude = explorationDestination.second,
            suggestedLongitude = explorationDestination.third
        ))

        return predictions.sortedByDescending { it.confidence }
    }

    private fun suggestNewPOIs(trips: List<TripEntity>, existingPois: List<POIEntity>): List<POISuggestion> {
        val suggestions = mutableListOf<POISuggestion>()

        // Raggruppa POI per area geografica
        val poiClusters = clusterPOIs(existingPois)

        // Per ogni cluster, suggerisci POI simili nelle vicinanze
        poiClusters.forEach { cluster ->
            val nearbyTrips = trips.filter { trip ->
                calculateDistance(trip.destinationLatitude, trip.destinationLongitude,
                    cluster.centerLat, cluster.centerLng) <= 20.0 // 20km
            }

            if (nearbyTrips.isNotEmpty()) {
                val poiSuggestions = generatePOISuggestionsForCluster(cluster, nearbyTrips)
                suggestions.addAll(poiSuggestions)
            }
        }

        // Suggerisci POI per destinazioni senza POI esistenti
        val destinationsWithoutPOIs = trips.filter { trip ->
            existingPois.none { poi ->
                calculateDistance(trip.destinationLatitude, trip.destinationLongitude,
                    poi.latitude, poi.longitude) <= 5.0 // 5km
            }
        }

        destinationsWithoutPOIs.forEach { trip ->
            suggestions.addAll(generateGenericPOISuggestions(trip))
        }

        return suggestions.distinctBy { "${it.name}_${it.latitude}_${it.longitude}" }
            .sortedByDescending { it.confidence }
            .take(10)
    }

    private data class POICluster(
        val centerLat: Double,
        val centerLng: Double,
        val pois: List<POIEntity>,
        val dominantCategory: String
    )

    private fun clusterPOIs(pois: List<POIEntity>): List<POICluster> {
        if (pois.isEmpty()) return emptyList()

        val clusters = mutableListOf<POICluster>()
        val processed = mutableSetOf<POIEntity>()

        pois.forEach { poi ->
            if (poi in processed) return@forEach

            val nearbyPOIs = pois.filter { other ->
                other !in processed &&
                        calculateDistance(poi.latitude, poi.longitude, other.latitude, other.longitude) <= 10.0 // 10km
            }

            if (nearbyPOIs.size >= 2) {
                val centerLat = nearbyPOIs.map { it.latitude }.average()
                val centerLng = nearbyPOIs.map { it.longitude }.average()
                val category = categorizePOI(poi.name)

                clusters.add(POICluster(centerLat, centerLng, nearbyPOIs, category))
                processed.addAll(nearbyPOIs)
            }
        }

        return clusters
    }

    private fun generatePOISuggestionsForCluster(cluster: POICluster, nearbyTrips: List<TripEntity>): List<POISuggestion> {
        val suggestions = mutableListOf<POISuggestion>()
        nearbyTrips.groupingBy { it.type }.eachCount().maxByOrNull { it.value }?.key ?: "Viaggio locale"

        when (cluster.dominantCategory) {
            "Ristorante" -> {
                suggestions.add(POISuggestion(
                    name = "Ristorante tradizionale locale",
                    latitude = cluster.centerLat + (Random().nextDouble() - 0.5) * 0.01,
                    longitude = cluster.centerLng + (Random().nextDouble() - 0.5) * 0.01,
                    category = "Ristorante",
                    confidence = 0.8,
                    reasoning = "Hai già ${cluster.pois.size} ristoranti in questa zona",
                    estimatedDistance = Random().nextDouble() * 2.0 + 0.5
                ))
            }
            "Attrazione" -> {
                suggestions.add(POISuggestion(
                    name = "Punto panoramico",
                    latitude = cluster.centerLat + (Random().nextDouble() - 0.5) * 0.01,
                    longitude = cluster.centerLng + (Random().nextDouble() - 0.5) * 0.01,
                    category = "Attrazione",
                    confidence = 0.75,
                    reasoning = "Zona ricca di attrazioni turistiche",
                    estimatedDistance = Random().nextDouble() * 3.0 + 1.0
                ))
            }
            "Alloggio" -> {
                suggestions.add(POISuggestion(
                    name = "B&B caratteristico",
                    latitude = cluster.centerLat + (Random().nextDouble() - 0.5) * 0.01,
                    longitude = cluster.centerLng + (Random().nextDouble() - 0.5) * 0.01,
                    category = "Alloggio",
                    confidence = 0.7,
                    reasoning = "Zona con ${cluster.pois.size} alloggi già testati",
                    estimatedDistance = Random().nextDouble() * 1.5 + 0.3
                ))
            }
        }

        return suggestions
    }

    private fun generateGenericPOISuggestions(trip: TripEntity): List<POISuggestion> {
        val suggestions = mutableListOf<POISuggestion>()
        val random = Random()

        when (trip.type) {
            "Viaggio di più giorni" -> {
                suggestions.addAll(listOf(
                    POISuggestion(
                        name = "Centro storico",
                        latitude = trip.destinationLatitude + (random.nextDouble() - 0.5) * 0.005,
                        longitude = trip.destinationLongitude + (random.nextDouble() - 0.5) * 0.005,
                        category = "Attrazione",
                        confidence = 0.85,
                        reasoning = "I centri storici sono sempre interessanti per viaggi lunghi",
                        estimatedDistance = random.nextDouble() * 2.0 + 0.5
                    ),
                    POISuggestion(
                        name = "Mercato locale",
                        latitude = trip.destinationLatitude + (random.nextDouble() - 0.5) * 0.008,
                        longitude = trip.destinationLongitude + (random.nextDouble() - 0.5) * 0.008,
                        category = "Shopping",
                        confidence = 0.7,
                        reasoning = "Esperienza culturale autentica",
                        estimatedDistance = random.nextDouble() * 3.0 + 1.0
                    )
                ))
            }
            "Gita giornaliera" -> {
                suggestions.add(POISuggestion(
                    name = "Area picnic",
                    latitude = trip.destinationLatitude + (random.nextDouble() - 0.5) * 0.01,
                    longitude = trip.destinationLongitude + (random.nextDouble() - 0.5) * 0.01,
                    category = "Natura",
                    confidence = 0.65,
                    reasoning = "Perfetto per una gita giornaliera",
                    estimatedDistance = random.nextDouble() * 5.0 + 2.0
                ))
            }
            "Viaggio locale" -> {
                suggestions.add(POISuggestion(
                    name = "Caffè caratteristico",
                    latitude = trip.destinationLatitude + (random.nextDouble() - 0.5) * 0.003,
                    longitude = trip.destinationLongitude + (random.nextDouble() - 0.5) * 0.003,
                    category = "Ristorante",
                    confidence = 0.6,
                    reasoning = "Scopri la zona con una pausa caffè",
                    estimatedDistance = random.nextDouble() * 1.0 + 0.2
                ))
            }
        }

        return suggestions
    }

    // Utility functions
    private fun calculateDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val earthRadius = 6371.0 // km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLng / 2) * sin(dLng / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c
    }

    private fun categorizePOI(name: String): String {
        val lowerName = name.lowercase()
        return when {
            lowerName.contains("ristorante") || lowerName.contains("pizzeria") || lowerName.contains("trattoria") -> "Ristorante"
            lowerName.contains("hotel") || lowerName.contains("b&b") || lowerName.contains("ostello") -> "Alloggio"
            lowerName.contains("museo") || lowerName.contains("chiesa") || lowerName.contains("castello") -> "Attrazione"
            lowerName.contains("parco") || lowerName.contains("giardino") || lowerName.contains("natura") -> "Natura"
            lowerName.contains("negozio") || lowerName.contains("mercato") || lowerName.contains("shopping") -> "Shopping"
            else -> "Generale"
        }
    }

    private fun getSeasonalTripType(month: Int): String {
        return when (month) {
            11, 0, 1 -> "Viaggio di più giorni" // Inverno - viaggi più lunghi
            2, 3, 4 -> "Gita giornaliera" // Primavera - gite fuori porta
            5, 6, 7 -> "Viaggio di più giorni" // Estate - vacanze
            8, 9, 10 -> "Viaggio locale" // Autunno - esplorazioni locali
            else -> "Viaggio locale"
        }
    }

    private fun generateSeasonalDestination(month: Int, trips: List<TripEntity>): Triple<String, Double, Double> {
        val avgLat = trips.map { it.destinationLatitude }.average()
        val avgLng = trips.map { it.destinationLongitude }.average()

        return when (month) {
            11, 0, 1 -> Triple("Destinazione invernale", avgLat - 0.5, avgLng) // Sud per il caldo
            2, 3, 4 -> Triple("Destinazione primaverile", avgLat + 0.2, avgLng + 0.2)
            5, 6, 7 -> Triple("Destinazione estiva", avgLat + 1.0, avgLng) // Nord per il fresco o mare
            8, 9, 10 -> Triple("Destinazione autunnale", avgLat, avgLng + 0.3)
            else -> Triple("Destinazione locale", avgLat, avgLng)
        }
    }

    private fun generateExplorationDestination(trips: List<TripEntity>, clusters: List<DestinationCluster>): Triple<String, Double, Double> {
        val avgLat = trips.map { it.destinationLatitude }.average()
        val avgLng = trips.map { it.destinationLongitude }.average()

        // Trova una direzione non ancora esplorata
        val directions = listOf(
            Triple("Nord", avgLat + 1.0, avgLng),
            Triple("Sud", avgLat - 1.0, avgLng),
            Triple("Est", avgLat, avgLng + 1.0),
            Triple("Ovest", avgLat, avgLng - 1.0)
        )

        val leastExploredDirection = directions.minByOrNull { direction ->
            clusters.sumOf { cluster ->
                calculateDistance(direction.second, direction.third, cluster.centerLat, cluster.centerLng)
            }
        } ?: directions.first()

        return Triple("Nuova zona a ${leastExploredDirection.first}", leastExploredDirection.second, leastExploredDirection.third)
    }

    private fun getMonthName(month: Int): String {
        val months = arrayOf("Gennaio", "Febbraio", "Marzo", "Aprile", "Maggio", "Giugno",
            "Luglio", "Agosto", "Settembre", "Ottobre", "Novembre", "Dicembre")
        return months[month]
    }

    private fun createEmptyAnalysis(): TravelAnalysis {
        return TravelAnalysis(
            totalTrips = 0,
            averageTripsPerMonth = 0.0,
            favoriteDestinationType = "Nessun dato",
            averageTripDuration = 0.0,
            mostActiveMonth = Calendar.getInstance().get(Calendar.MONTH),
            averageDistancePerTrip = 0.0,
            nextPredictedTripDate = System.currentTimeMillis() + (30 * 24 * 60 * 60 * 1000L),
            tripPredictions = emptyList(),
            poiSuggestions = emptyList()
        )
    }

    private data class TravelStatistics(
        val avgTripsPerMonth: Double,
        val favoriteType: String,
        val avgDuration: Double,
        val mostActiveMonth: Int,
        val avgDistance: Double,
        val nextPredictedDate: Long,
        val typeFrequency: Map<String, Int>,
        val monthlyPattern: Map<Int, Int>,
        val destinationClusters: List<DestinationCluster>
    )

    private data class DestinationCluster(
        val centerLat: Double,
        val centerLng: Double,
        val radius: Double,
        val tripCount: Int,
        val dominantType: String
    )

    private fun calculateStatistics(trips: List<TripEntity>): TravelStatistics {
        val calendar = Calendar.getInstance()
        val now = System.currentTimeMillis()

        // Calcola frequenza per tipo di viaggio
        val typeFrequency = trips.groupingBy { it.type }.eachCount()
        val favoriteType = typeFrequency.maxByOrNull { it.value }?.key ?: "Viaggio locale"

        // Calcola pattern mensili
        val monthlyPattern = mutableMapOf<Int, Int>()
        trips.forEach { trip ->
            calendar.timeInMillis = trip.startDate
            val month = calendar.get(Calendar.MONTH)
            monthlyPattern[month] = monthlyPattern.getOrDefault(month, 0) + 1
        }

        val mostActiveMonth = monthlyPattern.maxByOrNull { it.value }?.key ?: Calendar.getInstance().get(Calendar.MONTH)

        // Calcola durata media
        val avgDuration = trips.map { (it.endDate - it.startDate).toDouble() / (1000 * 60 * 60 * 24) }.average()

        // Calcola distanza media
        val avgDistance = trips.map { it.trackedDistance }.filter { it > 0 }.let { distances ->
            if (distances.isNotEmpty()) distances.average() else 0.0
        }

        // Calcola viaggi per mese
        val firstTripDate = trips.minOfOrNull { it.startDate } ?: now
        val monthsDiff = max(1, ((now - firstTripDate) / (1000L * 60 * 60 * 24 * 30)).toInt())
        val avgTripsPerMonth = trips.size.toDouble() / monthsDiff

        // Predici prossima data di viaggio basata sui pattern storici
        val avgDaysBetweenTrips = if (trips.size > 1) {
            val sortedTrips = trips.sortedBy { it.startDate }
            val intervals = sortedTrips.zipWithNext { a, b ->
                (b.startDate - a.endDate).toDouble() / (1000 * 60 * 60 * 24)
            }
            intervals.average()
        } else 30.0

        val lastTripEnd = trips.maxOfOrNull { it.endDate } ?: now
        val nextPredictedDate = lastTripEnd + (avgDaysBetweenTrips * 1000 * 60 * 60 * 24).toLong()

        // Clustering delle destinazioni
        val clusters = clusterDestinations(trips)

        return TravelStatistics(
            avgTripsPerMonth = avgTripsPerMonth,
            favoriteType = favoriteType,
            avgDuration = avgDuration,
            mostActiveMonth = mostActiveMonth,
            avgDistance = avgDistance,
            nextPredictedDate = nextPredictedDate,
            typeFrequency = typeFrequency,
            monthlyPattern = monthlyPattern,
            destinationClusters = clusters
        )
    }
}