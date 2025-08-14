package com.example.travel_companion.presentation.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.Observer
import com.example.travel_companion.data.local.entity.TripEntity
import com.example.travel_companion.domain.model.TripStatus
import com.example.travel_companion.data.repository.TripRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val tripRepository: TripRepository
) : ViewModel() {

    private val _completedTrips = MutableStateFlow<List<TripEntity>>(emptyList())
    val completedTrips: StateFlow<List<TripEntity>> = _completedTrips.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

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

    override fun onCleared() {
        super.onCleared()
        // Rimuovi l'observer quando il ViewModel viene distrutto
        tripsObserver?.let { observer ->
            tripRepository.getAllTrips().removeObserver(observer)
        }
    }
}