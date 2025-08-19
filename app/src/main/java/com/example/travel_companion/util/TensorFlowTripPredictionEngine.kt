package com.example.travel_companion.util

import com.example.travel_companion.data.local.entity.TripEntity
import com.example.travel_companion.domain.model.TripStatus
import com.example.travel_companion.domain.model.TripAnalysis
import com.example.travel_companion.domain.model.TripPrediction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import timber.log.Timber
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.util.*
import kotlin.math.*
import android.content.Context

class TensorFlowTripPredictionEngine(private val context: Context) {

    companion object {
        private const val TAG = "TFTripPrediction"
        private const val MODEL_FILE = "trip_prediction_model.tflite"
        private const val INPUT_SIZE = 10 // Features per l'input
        private const val OUTPUT_SIZE = 4 // Predizioni di output
    }

    private var interpreter: Interpreter? = null

    init {
        loadModel()
    }

    private fun loadModel() {
        try {
            val modelBuffer = loadModelFile()
            interpreter = Interpreter(modelBuffer)
            Timber.tag(TAG).d("Modello TensorFlow caricato con successo")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Errore nel caricamento del modello TensorFlow")
            // Fallback a sistema statistico
        }
    }

    private fun loadModelFile(): ByteBuffer {
        val assetFileDescriptor = context.assets.openFd(MODEL_FILE)
        val fileInputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = fileInputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    suspend fun analyzeAndPredict(trips: List<TripEntity>): TripAnalysis = withContext(Dispatchers.Default) {
        Timber.tag(TAG).d("=== INIZIO ANALISI TENSORFLOW ===")
        Timber.tag(TAG).d("Viaggi totali ricevuti: ${trips.size}")

        val completedTrips = trips.filter { it.status == TripStatus.FINISHED }
        Timber.tag(TAG).d("Viaggi completati: ${completedTrips.size}")

        if (completedTrips.isEmpty()) {
            Timber.tag(TAG).w("Nessun viaggio completato - ritorno analisi vuota")
            return@withContext createEmptyAnalysis()
        }

        val statistics = calculateAdvancedStatistics(completedTrips)
        val tripPredictions = if (interpreter != null && completedTrips.size >= 5) {
            // Usa TensorFlow se disponibile e con dati sufficienti
            predictWithTensorFlow(completedTrips, statistics)
        } else {
            // Fallback al sistema statistico migliorato
            predictWithStatistics(completedTrips, statistics)
        }

        Timber.tag(TAG).d("Previsioni generate: ${tripPredictions.size}")
        Timber.tag(TAG).d("=== FINE ANALISI TENSORFLOW ===")

        TripAnalysis(
            totalTrips = completedTrips.size,
            averageTripsPerMonth = statistics.avgTripsPerMonth,
            favoriteDestinationType = statistics.favoriteType,
            averageTripDuration = statistics.avgDuration,
            mostActiveMonth = statistics.mostActiveMonth,
            averageDistancePerTrip = statistics.avgDistance,
            nextPredictedTripDate = statistics.nextPredictedDate,
            tripPredictions = tripPredictions
        )
    }

    private fun predictWithTensorFlow(trips: List<TripEntity>, stats: TripStatistics): List<TripPrediction> {
        val predictions = mutableListOf<TripPrediction>()

        try {
            // Prepara features per il modello
            val inputData = prepareInputFeatures(trips, stats)
            val outputData = Array(1) { FloatArray(OUTPUT_SIZE) }

            interpreter?.run(inputData, outputData)

            val output = outputData[0]
            Timber.tag(TAG).d("Output TensorFlow: ${output.contentToString()}")

            // Interpreta l'output del modello
            predictions.addAll(interpretTensorFlowOutput(output, trips, stats))

        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Errore nella predizione TensorFlow")
            // Fallback al sistema statistico
            return predictWithStatistics(trips, stats)
        }

        return predictions.sortedByDescending { it.confidence }
    }

    private fun prepareInputFeatures(trips: List<TripEntity>, stats: TripStatistics): Array<FloatArray> {
        val features = FloatArray(INPUT_SIZE)

        // Feature 0: Numero viaggi normalizzato
        features[0] = minOf(trips.size.toFloat() / 50f, 1f)

        // Feature 1: Viaggi per mese normalizzati
        features[1] = minOf(stats.avgTripsPerMonth.toFloat() / 10f, 1f)

        // Feature 2: Durata media normalizzata (in giorni)
        features[2] = minOf(stats.avgDuration.toFloat() / 30f, 1f)

        // Feature 3: Distanza media normalizzata (in km)
        features[3] = minOf(stats.avgDistance.toFloat() / 1000f, 1f)

        // Feature 4: Mese corrente normalizzato
        features[4] = Calendar.getInstance().get(Calendar.MONTH) / 11f

        // Feature 5: Stagionalità (0-3)
        features[5] = getSeason() / 3f

        // Feature 6: Tipo preferito codificato
        features[6] = encodeTripType(stats.favoriteType)

        // Feature 7: Variabilità delle destinazioni
        features[7] = calculateDestinationVariability(trips)

        // Feature 8: Trend recente (ultimi 3 mesi)
        features[8] = calculateRecentTrend(trips)

        // Feature 9: Giorni dall'ultimo viaggio normalizzati
        features[9] = calculateDaysSinceLastTrip(trips) / 365f

        return arrayOf(features)
    }

    private fun interpretTensorFlowOutput(output: FloatArray, trips: List<TripEntity>, stats: TripStatistics): List<TripPrediction> {
        val predictions = mutableListOf<TripPrediction>()
        val calendar = Calendar.getInstance()

        // Output[0]: Probabilità prossimo viaggio entro 30 giorni
        val shortTermProbability = output[0]
        if (shortTermProbability > 0.3) {
            predictions.add(createShortTermPrediction(shortTermProbability, trips, stats))
        }

        // Output[1]: Probabilità viaggio stagionale
        val seasonalProbability = output[1]
        if (seasonalProbability > 0.4) {
            predictions.add(createSeasonalPrediction(seasonalProbability, trips, stats))
        }

        // Output[2]: Probabilità esplorazione nuova area
        val explorationProbability = output[2]
        if (explorationProbability > 0.2) {
            predictions.add(createExplorationPrediction(explorationProbability, trips, stats))
        }

        // Output[3]: Probabilità ritorno a destinazione preferita
        val returnProbability = output[3]
        if (returnProbability > 0.5) {
            predictions.add(createReturnPrediction(returnProbability, trips, stats))
        }

        return predictions
    }

    private fun createShortTermPrediction(confidence: Float, trips: List<TripEntity>, stats: TripStatistics): TripPrediction {
        val baseDate = System.currentTimeMillis()
        val daysOffset = (confidence * 30).toInt() // Entro 30 giorni
        val startDate = baseDate + (daysOffset * 24 * 60 * 60 * 1000L)

        return TripPrediction(
            suggestedDestination = "Destinazione vicina",
            predictedType = stats.favoriteType,
            suggestedStartDate = startDate,
            suggestedEndDate = startDate + (stats.avgDuration * 24 * 60 * 60 * 1000).toLong(),
            confidence = confidence.toDouble(),
            reasoning = "AI prevede un viaggio breve basato sui tuoi pattern recenti",
            suggestedLatitude = getAverageLatitude(trips) + (Math.random() - 0.5) * 0.1,
            suggestedLongitude = getAverageLongitude(trips) + (Math.random() - 0.5) * 0.1
        )
    }

    private fun createSeasonalPrediction(confidence: Float, trips: List<TripEntity>, stats: TripStatistics): TripPrediction {
        val season = getSeason()
        val seasonalType = getSeasonalTripType(season)
        val baseDate = System.currentTimeMillis()
        val startDate = baseDate + (14 * 24 * 60 * 60 * 1000L) // +2 settimane

        return TripPrediction(
            suggestedDestination = "Destinazione ${getSeasonName(season)}",
            predictedType = seasonalType,
            suggestedStartDate = startDate,
            suggestedEndDate = startDate + (stats.avgDuration * 24 * 60 * 60 * 1000).toLong(),
            confidence = confidence.toDouble(),
            reasoning = "Pattern stagionale rilevato dall'AI per ${getSeasonName(season)}",
            suggestedLatitude = getSeasonalLatitude(season, trips),
            suggestedLongitude = getSeasonalLongitude(season, trips)
        )
    }

    private fun createExplorationPrediction(confidence: Float, trips: List<TripEntity>, stats: TripStatistics): TripPrediction {
        val baseDate = System.currentTimeMillis()
        val startDate = baseDate + (21 * 24 * 60 * 60 * 1000L) // +3 settimane
        val explorationArea = findUnexploredArea(trips)

        return TripPrediction(
            suggestedDestination = "Nuova area da esplorare",
            predictedType = stats.favoriteType,
            suggestedStartDate = startDate,
            suggestedEndDate = startDate + (stats.avgDuration * 24 * 60 * 60 * 1000).toLong(),
            confidence = confidence.toDouble(),
            reasoning = "L'AI suggerisce di esplorare una nuova zona",
            suggestedLatitude = explorationArea.first,
            suggestedLongitude = explorationArea.second
        )
    }

    private fun createReturnPrediction(confidence: Float, trips: List<TripEntity>, stats: TripStatistics): TripPrediction {
        val favoriteDestination = findMostVisitedDestination(trips)
        val baseDate = System.currentTimeMillis()
        val startDate = baseDate + (7 * 24 * 60 * 60 * 1000L) // +1 settimana

        return TripPrediction(
            suggestedDestination = "Ritorno alla destinazione preferita",
            predictedType = stats.favoriteType,
            suggestedStartDate = startDate,
            suggestedEndDate = startDate + (stats.avgDuration * 24 * 60 * 60 * 1000).toLong(),
            confidence = confidence.toDouble(),
            reasoning = "L'AI prevede un ritorno alla tua destinazione preferita",
            suggestedLatitude = favoriteDestination.first,
            suggestedLongitude = favoriteDestination.second
        )
    }

    // Utility functions
    private fun predictWithStatistics(trips: List<TripEntity>, stats: TripStatistics): List<TripPrediction> {
        // Fallback al sistema statistico originale (semplificato)
        val predictions = mutableListOf<TripPrediction>()
        val baseDate = System.currentTimeMillis()

        predictions.add(TripPrediction(
            suggestedDestination = "Destinazione statistica",
            predictedType = stats.favoriteType,
            suggestedStartDate = baseDate + (14 * 24 * 60 * 60 * 1000L),
            suggestedEndDate = baseDate + ((14 + stats.avgDuration) * 24 * 60 * 60 * 1000).toLong(),
            confidence = 0.6,
            reasoning = "Predizione basata su analisi statistica",
            suggestedLatitude = getAverageLatitude(trips),
            suggestedLongitude = getAverageLongitude(trips)
        ))

        return predictions
    }

    private fun encodeTripType(type: String): Float {
        return when (type) {
            "Viaggio locale" -> 0.0f
            "Gita giornaliera" -> 0.33f
            "Viaggio di più giorni" -> 0.66f
            else -> 1.0f
        }
    }

    private fun getSeason(): Int {
        val month = Calendar.getInstance().get(Calendar.MONTH)
        return when (month) {
            11, 0, 1 -> 0 // Inverno
            2, 3, 4 -> 1   // Primavera
            5, 6, 7 -> 2   // Estate
            else -> 3      // Autunno
        }
    }

    private fun getSeasonName(season: Int): String {
        return when (season) {
            0 -> "Inverno"
            1 -> "Primavera"
            2 -> "Estate"
            else -> "Autunno"
        }
    }

    private fun getSeasonalTripType(season: Int): String {
        return when (season) {
            0, 2 -> "Viaggio di più giorni" // Inverno/Estate - vacanze
            1 -> "Gita giornaliera"        // Primavera - gite
            else -> "Viaggio locale"       // Autunno - locale
        }
    }

    private fun calculateDestinationVariability(trips: List<TripEntity>): Float {
        if (trips.size < 2) return 0f

        val latStd = trips.map { it.destinationLatitude }.let { lats ->
            val mean = lats.average()
            sqrt(lats.map { (it - mean).pow(2) }.average())
        }

        val lngStd = trips.map { it.destinationLongitude }.let { lngs ->
            val mean = lngs.average()
            sqrt(lngs.map { (it - mean).pow(2) }.average())
        }

        return minOf((latStd + lngStd).toFloat() / 2f, 1f)
    }

    private fun calculateRecentTrend(trips: List<TripEntity>): Float {
        val threeMonthsAgo = System.currentTimeMillis() - (90 * 24 * 60 * 60 * 1000L)
        val recentTrips = trips.filter { it.startDate >= threeMonthsAgo }
        return minOf(recentTrips.size.toFloat() / 10f, 1f)
    }

    private fun calculateDaysSinceLastTrip(trips: List<TripEntity>): Float {
        val lastTrip = trips.maxByOrNull { it.endDate }
        return if (lastTrip != null) {
            val daysSince = (System.currentTimeMillis() - lastTrip.endDate) / (24 * 60 * 60 * 1000f)
            minOf(daysSince, 365f)
        } else 365f
    }

    private fun getAverageLatitude(trips: List<TripEntity>): Double {
        return trips.map { it.destinationLatitude }.average()
    }

    private fun getAverageLongitude(trips: List<TripEntity>): Double {
        return trips.map { it.destinationLongitude }.average()
    }

    private fun getSeasonalLatitude(season: Int, trips: List<TripEntity>): Double {
        val avgLat = getAverageLatitude(trips)
        return when (season) {
            0 -> avgLat - 0.5 // Inverno - sud
            1 -> avgLat + 0.2 // Primavera - poco a nord
            2 -> avgLat + 0.8 // Estate - nord
            else -> avgLat    // Autunno - stesso posto
        }
    }

    private fun getSeasonalLongitude(season: Int, trips: List<TripEntity>): Double {
        val avgLng = getAverageLongitude(trips)
        return when (season) {
            2 -> avgLng + 0.5 // Estate - verso mare
            else -> avgLng
        }
    }

    private fun findUnexploredArea(trips: List<TripEntity>): Pair<Double, Double> {
        val avgLat = getAverageLatitude(trips)
        val avgLng = getAverageLongitude(trips)

        // Trova direzione meno esplorata
        val directions = listOf(
            Pair(avgLat + 1.0, avgLng),     // Nord
            Pair(avgLat - 1.0, avgLng),     // Sud
            Pair(avgLat, avgLng + 1.0),     // Est
            Pair(avgLat, avgLng - 1.0)      // Ovest
        )

        return directions.minByOrNull { direction ->
            trips.sumOf { trip ->
                val dist = calculateDistance(
                    direction.first, direction.second,
                    trip.destinationLatitude, trip.destinationLongitude
                )
                1.0 / (dist + 1.0) // Inverso della distanza
            }
        } ?: directions.first()
    }

    private fun findMostVisitedDestination(trips: List<TripEntity>): Pair<Double, Double> {
        if (trips.isEmpty()) return Pair(0.0, 0.0)

        // Raggruppa per prossimità e trova il cluster più grande
        val clusters = mutableListOf<List<TripEntity>>()
        val processed = mutableSetOf<TripEntity>()

        trips.forEach { trip ->
            if (trip in processed) return@forEach

            val nearbyTrips = trips.filter { other ->
                calculateDistance(
                    trip.destinationLatitude, trip.destinationLongitude,
                    other.destinationLatitude, other.destinationLongitude
                ) <= 20.0 // 20km
            }

            if (nearbyTrips.isNotEmpty()) {
                clusters.add(nearbyTrips)
                processed.addAll(nearbyTrips)
            }
        }

        val largestCluster = clusters.maxByOrNull { it.size } ?: listOf(trips.first())

        return Pair(
            largestCluster.map { it.destinationLatitude }.average(),
            largestCluster.map { it.destinationLongitude }.average()
        )
    }

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

    private fun calculateAdvancedStatistics(trips: List<TripEntity>): TripStatistics {
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

        // Predici prossima data
        val avgDaysBetweenTrips = if (trips.size > 1) {
            val sortedTrips = trips.sortedBy { it.startDate }
            val intervals = sortedTrips.zipWithNext { a, b ->
                (b.startDate - a.endDate).toDouble() / (1000 * 60 * 60 * 24)
            }
            intervals.average()
        } else 30.0

        val lastTripEnd = trips.maxOfOrNull { it.endDate } ?: now
        val nextPredictedDate = lastTripEnd + (avgDaysBetweenTrips * 1000 * 60 * 60 * 24).toLong()

        return TripStatistics(
            avgTripsPerMonth = avgTripsPerMonth,
            favoriteType = favoriteType,
            avgDuration = avgDuration,
            mostActiveMonth = mostActiveMonth,
            avgDistance = avgDistance,
            nextPredictedDate = nextPredictedDate,
            typeFrequency = typeFrequency,
            monthlyPattern = monthlyPattern,
            destinationClusters = emptyList()
        )
    }

    private fun createEmptyAnalysis(): TripAnalysis {
        return TripAnalysis(
            totalTrips = 0,
            averageTripsPerMonth = 0.0,
            favoriteDestinationType = "Nessun dato",
            averageTripDuration = 0.0,
            mostActiveMonth = Calendar.getInstance().get(Calendar.MONTH),
            averageDistancePerTrip = 0.0,
            nextPredictedTripDate = System.currentTimeMillis() + (30 * 24 * 60 * 60 * 1000L),
            tripPredictions = emptyList()
        )
    }

    private data class TripStatistics(
        val avgTripsPerMonth: Double,
        val favoriteType: String,
        val avgDuration: Double,
        val mostActiveMonth: Int,
        val avgDistance: Double,
        val nextPredictedDate: Long,
        val typeFrequency: Map<String, Int>,
        val monthlyPattern: Map<Int, Int>,
        val destinationClusters: List<Any>
    )

    fun close() {
        interpreter?.close()
    }
}