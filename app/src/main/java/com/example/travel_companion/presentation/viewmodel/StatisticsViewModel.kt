package com.example.travel_companion.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.Observer
import androidx.lifecycle.viewModelScope
import com.example.travel_companion.data.local.entity.TripEntity
import com.example.travel_companion.data.local.entity.CoordinateEntity
import com.example.travel_companion.domain.model.TripStatus
import com.example.travel_companion.data.repository.TripRepository
import com.example.travel_companion.data.local.dao.CoordinateDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val tripRepository: TripRepository,
    private val coordinateDao: CoordinateDao
) : ViewModel() {

    private val _completedTrips = MutableStateFlow<List<TripEntity>>(emptyList())
    val completedTrips: StateFlow<List<TripEntity>> = _completedTrips.asStateFlow()

    private val _isLoading = MutableStateFlow(false)

    private var tripsObserver: Observer<List<TripEntity>>? = null

    fun loadStatistics() {
        _isLoading.value = true

        // Rimuovi il precedente observer se esiste
        tripsObserver?.let { observer ->
            tripRepository.getAllTrips().removeObserver(observer)
        }

        // Crea un nuovo observer
        tripsObserver = Observer { allTrips ->
            _completedTrips.value = allTrips.filter { trip ->
                trip.status == TripStatus.FINISHED &&
                        trip.destinationLatitude != 0.0 &&
                        trip.destinationLongitude != 0.0
            }
            _isLoading.value = false
        }

        // Osserva i cambiamenti
        tripRepository.getAllTrips().observeForever(tripsObserver!!)
    }

    /**
     * Recupera tutte le coordinate per i viaggi completati
     */
    suspend fun getAllCoordinatesForCompletedTrips(): List<CoordinateEntity> {
        return withContext(Dispatchers.IO) {
            val completedTrips = _completedTrips.value
            val allCoordinates = mutableListOf<CoordinateEntity>()

            completedTrips.forEach { trip ->
                val coordinates = coordinateDao.getCoordinatesForTrip(trip.id)
                allCoordinates.addAll(coordinates)
            }

            allCoordinates
        }
    }

    /**
     * Campiona le coordinate per ridurre il carico se necessario
     */
    fun sampleCoordinates(coordinates: List<CoordinateEntity>, sampleRate: Int = 5): List<CoordinateEntity> {
        return if (coordinates.size > 1000) {
            coordinates.filterIndexed { index, _ -> index % sampleRate == 0 }
        } else {
            coordinates
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Rimuovi l'observer quando il ViewModel viene distrutto
        tripsObserver?.let { observer ->
            tripRepository.getAllTrips().removeObserver(observer)
        }
    }
}