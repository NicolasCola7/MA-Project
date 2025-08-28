package com.example.travel_companion.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.Observer
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

    // Holds the list of completed trips
    private val _completedTrips = MutableStateFlow<List<TripEntity>>(emptyList())
    val completedTrips: StateFlow<List<TripEntity>> = _completedTrips.asStateFlow()

    // Loading state
    private val _isLoading = MutableStateFlow(false)

    // Observer for live updates from the repository
    private var tripsObserver: Observer<List<TripEntity>>? = null

    /**
     * Loads statistics by observing all trips and filtering only the completed ones
     */
    fun loadStatistics() {
        _isLoading.value = true

        // Remove previous observer if it exists
        tripsObserver?.let { observer ->
            tripRepository.getAllTrips().removeObserver(observer)
        }

        // Create a new observer
        tripsObserver = Observer { allTrips ->
            _completedTrips.value = allTrips.filter { trip ->
                trip.status == TripStatus.FINISHED &&
                        trip.destinationLatitude != 0.0 &&
                        trip.destinationLongitude != 0.0
            }
            _isLoading.value = false
        }

        // Observe changes forever
        tripRepository.getAllTrips().observeForever(tripsObserver!!)
    }

    /**
     * Retrieves all coordinates for completed trips
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
     * Samples coordinates to reduce processing load if necessary
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
        // Remove the observer when the ViewModel is destroyed
        tripsObserver?.let { observer ->
            tripRepository.getAllTrips().removeObserver(observer)
        }
    }
}
