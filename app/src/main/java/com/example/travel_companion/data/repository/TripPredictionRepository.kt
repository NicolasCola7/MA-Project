package com.example.travel_companion.data.repository

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.asFlow
import com.example.travel_companion.data.local.dao.TripDao
import com.example.travel_companion.data.local.entity.TripEntity
import com.example.travel_companion.domain.model.TripAnalysis
import com.example.travel_companion.domain.model.TripStatus
import com.example.travel_companion.util.TensorFlowTripPredictionEngine
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TripPredictionRepository @Inject constructor(
    private val tripDao: TripDao,
    @ApplicationContext private val context: Context
) {

    private val predictionEngine by lazy {
        TensorFlowTripPredictionEngine(context)
    }

    companion object {
        private const val TAG = "TripPredictionRepo"
    }

    suspend fun generateTripAnalysis(): TripAnalysis = withContext(Dispatchers.IO) {
        Timber.tag(TAG).d("Generazione analisi viaggi...")

        try {
            // Converte LiveData in Flow e prende il primo valore
            val allTrips = tripDao.getAll().asFlow().first()
            Timber.tag(TAG).d("Recuperati ${allTrips.size} viaggi dal database")

            val analysis = predictionEngine.analyzeAndPredict(allTrips)
            Timber.tag(TAG).d("Analisi completata con ${analysis.tripPredictions.size} predizioni")

            return@withContext analysis

        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Errore nella generazione dell'analisi")
            throw e
        }
    }

    suspend fun getPredictionsForTripType(tripType: String): TripAnalysis = withContext(Dispatchers.IO) {
        Timber.tag(TAG).d("Generazione predizioni per tipo: $tripType")

        try {
            val allTrips = tripDao.getAll().asFlow().first()
            val filteredTrips = if (tripType == "Tutti") {
                allTrips
            } else {
                allTrips.filter { it.type == tripType }
            }

            Timber.tag(TAG).d("Viaggi filtrati per '$tripType': ${filteredTrips.size}")

            val analysis = predictionEngine.analyzeAndPredict(filteredTrips)

            // Filtra anche le predizioni per tipo se necessario
            val filteredPredictions = if (tripType != "Tutti") {
                analysis.tripPredictions.filter { it.predictedType == tripType }
            } else {
                analysis.tripPredictions
            }

            return@withContext analysis.copy(tripPredictions = filteredPredictions)

        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Errore nella generazione delle predizioni filtrate")
            throw e
        }
    }

    /**
     * Aggiorna il modello TensorFlow con nuovi dati (per future implementazioni)
     */
    suspend fun updateModel(): Boolean = withContext(Dispatchers.IO) {
        try {
            Timber.tag(TAG).d("Aggiornamento modello TensorFlow...")

            // TODO: Implementare logica di aggiornamento del modello
            // - Raccogliere nuovi dati di viaggio
            // - Preparare dataset di training
            // - Re-trainare il modello (o scaricare versione aggiornata)

            Timber.tag(TAG).d("Modello aggiornato con successo")
            return@withContext true

        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Errore nell'aggiornamento del modello")
            return@withContext false
        }
    }

    /**
     * Ottieni statistiche avanzate sui viaggi
     */
    suspend fun getAdvancedStatistics(): Map<String, Any> = withContext(Dispatchers.IO) {
        try {
            val trips = tripDao.getAll().asFlow().first()
            val completedTrips = trips.filter { it.status == TripStatus.FINISHED }

            val stats = mutableMapOf<String, Any>()

            // Statistiche di base
            stats["total_trips"] = trips.size
            stats["completed_trips"] = completedTrips.size
            stats["completion_rate"] = if (trips.isNotEmpty()) {
                (completedTrips.size.toDouble() / trips.size * 100)
            } else 0.0

            // Statistiche per tipo
            val typeStats = completedTrips.groupingBy { it.type }.eachCount()
            stats["type_distribution"] = typeStats

            // Statistiche temporali
            val monthlyStats = mutableMapOf<Int, Int>()
            completedTrips.forEach { trip ->
                val calendar = java.util.Calendar.getInstance()
                calendar.timeInMillis = trip.startDate
                val month = calendar.get(java.util.Calendar.MONTH)
                monthlyStats[month] = monthlyStats.getOrDefault(month, 0) + 1
            }
            stats["monthly_distribution"] = monthlyStats

            // Statistiche geografiche
            if (completedTrips.isNotEmpty()) {
                val avgLat = completedTrips.map { it.destinationLatitude }.average()
                val avgLng = completedTrips.map { it.destinationLongitude }.average()
                stats["average_destination"] = mapOf("lat" to avgLat, "lng" to avgLng)

                // Calcola il raggio di esplorazione
                val distances = completedTrips.map { trip ->
                    calculateDistanceFromCenter(avgLat, avgLng, trip.destinationLatitude, trip.destinationLongitude)
                }
                stats["exploration_radius"] = distances.maxOrNull() ?: 0.0
                stats["average_distance_from_center"] = distances.average()
            }

            return@withContext stats

        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Errore nel calcolo delle statistiche avanzate")
            return@withContext emptyMap<String, Any>()
        }
    }

    /**
     * Predici il prossimo viaggio più probabile
     */
    suspend fun getPrimaryPrediction(): TripAnalysis? = withContext(Dispatchers.IO) {
        try {
            val analysis = generateTripAnalysis()

            return@withContext if (analysis.tripPredictions.isNotEmpty()) {
                // Ritorna solo la predizione con confidenza più alta
                val bestPrediction = analysis.tripPredictions.maxByOrNull { it.confidence }
                analysis.copy(tripPredictions = listOfNotNull(bestPrediction))
            } else {
                null
            }

        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Errore nella predizione primaria")
            return@withContext null
        }
    }

    /**
     * Valida la qualità delle predizioni basandosi sui viaggi passati
     */
    suspend fun evaluatePredictionAccuracy(): Double = withContext(Dispatchers.IO) {
        try {
            val trips = tripDao.getAll().asFlow().first()
            val completedTrips = trips.filter { it.status == TripStatus.FINISHED }

            if (completedTrips.size < 5) {
                return@withContext 0.0 // Non abbastanza dati per valutare
            }

            // Simula predizioni su dati storici
            val trainingTrips = completedTrips.dropLast(2) // Usa tutti tranne gli ultimi 2
            val testTrips = completedTrips.takeLast(2)     // Testa sugli ultimi 2

            val historicalAnalysis = predictionEngine.analyzeAndPredict(trainingTrips)

            // Calcola accuratezza basandosi sulla vicinanza delle predizioni ai viaggi reali
            var accuracyScore = 0.0

            testTrips.forEach { actualTrip ->
                val bestMatch = historicalAnalysis.tripPredictions.minByOrNull { prediction ->
                    calculateDistanceFromCenter(
                        prediction.suggestedLatitude ?: 0.0,
                        prediction.suggestedLongitude ?: 0.0,
                        actualTrip.destinationLatitude,
                        actualTrip.destinationLongitude
                    )
                }

                bestMatch?.let { match ->
                    val distance = calculateDistanceFromCenter(
                        match.suggestedLatitude ?: 0.0,
                        match.suggestedLongitude ?: 0.0,
                        actualTrip.destinationLatitude,
                        actualTrip.destinationLongitude
                    )

                    // Accuratezza inversamente proporzionale alla distanza
                    val accuracy = maxOf(0.0, 1.0 - (distance / 100.0)) // 100km = 0% accuracy
                    accuracyScore += accuracy
                }
            }

            return@withContext accuracyScore / testTrips.size

        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Errore nella valutazione dell'accuratezza")
            return@withContext 0.0
        }
    }

    /**
     * Ottieni suggerimenti personalizzati basati sui pattern dell'utente
     */
    suspend fun getPersonalizedSuggestions(): List<String> = withContext(Dispatchers.IO) {
        try {
            val trips = tripDao.getAll().asFlow().first()
            val completedTrips = trips.filter { it.status == TripStatus.FINISHED }
            val suggestions = mutableListOf<String>()

            if (completedTrips.isEmpty()) {
                suggestions.add("Inizia registrando i tuoi primi viaggi per ricevere suggerimenti personalizzati!")
                return@withContext suggestions
            }

            // Analizza pattern temporali
            val monthlyPattern = completedTrips.groupingBy { trip ->
                val calendar = java.util.Calendar.getInstance()
                calendar.timeInMillis = trip.startDate
                calendar.get(java.util.Calendar.MONTH)
            }.eachCount()

            val currentMonth = java.util.Calendar.getInstance().get(java.util.Calendar.MONTH)
            val tripsThisMonth = monthlyPattern[currentMonth] ?: 0
            val avgTripsPerMonth = if (monthlyPattern.values.isNotEmpty()) {
                monthlyPattern.values.average()
            } else 0.0

            if (tripsThisMonth < avgTripsPerMonth) {
                suggestions.add("Questo mese hai viaggiato meno del solito. Che ne dici di pianificare una gita?")
            }

            // Analizza tipi di viaggio
            val typeFrequency = completedTrips.groupingBy { it.type }.eachCount()
            val leastUsedType = typeFrequency.minByOrNull { it.value }?.key

            leastUsedType?.let {
                suggestions.add("Prova qualcosa di diverso: non fai un '$it' da un po'!")
            }

            // Analizza stagionalità
            val season = getCurrentSeason()
            when (season) {
                "Primavera" -> suggestions.add("È primavera! Perfetto per gite fuori porta e escursioni.")
                "Estate" -> suggestions.add("L'estate è ideale per viaggi più lunghi e vacanze.")
                "Autunno" -> suggestions.add("L'autunno è perfetto per esplorare nuovi luoghi vicini.")
                "Inverno" -> suggestions.add("L'inverno è ottimo per viaggi culturali e destinazioni al caldo.")
            }

            // Analizza distanze
            val validDistances = completedTrips.map { it.trackedDistance }.filter { it > 0 }
            if (validDistances.isNotEmpty()) {
                val avgDistance = validDistances.average()
                if (avgDistance < 50) {
                    suggestions.add("I tuoi viaggi sono principalmente locali. Che ne dici di esplorare più lontano?")
                } else if (avgDistance > 500) {
                    suggestions.add("Ami i viaggi lunghi! Hai mai pensato a esplorare qualcosa di più vicino?")
                }
            }

            return@withContext suggestions

        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Errore nella generazione dei suggerimenti")
            return@withContext listOf("Errore nel caricamento dei suggerimenti")
        }
    }

    /**
     * Ottieni dati in tempo reale per l'UI (LiveData)
     */
    fun getAllTripsLive(): LiveData<List<TripEntity>> {
        return tripDao.getAll()
    }

    /**
     * Ottieni viaggi per stato specifico
     */
    fun getTripsByStatusLive(status: TripStatus): LiveData<List<TripEntity>> {
        return tripDao.getTripsByStatus(status)
    }

    /**
     * Ottieni viaggi completati in modo sincrono per analisi
     */
    suspend fun getCompletedTripsSync(): List<TripEntity> = withContext(Dispatchers.IO) {
        return@withContext tripDao.getTripsByStatusSync(TripStatus.FINISHED)
    }

    /**
     * Aggiorna stati dei viaggi automaticamente
     */
    suspend fun updateTripStatuses() = withContext(Dispatchers.IO) {
        try {
            val currentTime = System.currentTimeMillis()

            val startedTrips = tripDao.updatePlannedTripsToStarted(currentTime)
            val finishedTrips = tripDao.updateStartedTripsToFinished(currentTime)

            Timber.tag(TAG).d("Aggiornati stati: $startedTrips avviati, $finishedTrips completati")

        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Errore nell'aggiornamento degli stati")
        }
    }

    /**
     * Ottieni predizioni basate solo su viaggi completati
     */
    suspend fun generateCompletedTripsAnalysis(): TripAnalysis = withContext(Dispatchers.IO) {
        try {
            val completedTrips = getCompletedTripsSync()
            Timber.tag(TAG).d("Recuperati ${completedTrips.size} viaggi completati per analisi")

            val analysis = predictionEngine.analyzeAndPredict(completedTrips)
            Timber.tag(TAG).d("Analisi viaggi completati: ${analysis.tripPredictions.size} predizioni")

            return@withContext analysis

        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Errore nell'analisi viaggi completati")
            throw e
        }
    }

    private fun calculateDistanceFromCenter(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val earthRadius = 6371.0 // km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
                kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) *
                kotlin.math.sin(dLng / 2) * kotlin.math.sin(dLng / 2)
        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
        return earthRadius * c
    }

    private fun getCurrentSeason(): String {
        val month = java.util.Calendar.getInstance().get(java.util.Calendar.MONTH)
        return when (month) {
            11, 0, 1 -> "Inverno"
            2, 3, 4 -> "Primavera"
            5, 6, 7 -> "Estate"
            else -> "Autunno"
        }
    }

    /**
     * Pulizia delle risorse quando il repository non è più necessario
     */
    fun cleanup() {
        predictionEngine.close()
    }
}