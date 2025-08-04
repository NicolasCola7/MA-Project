package com.example.travel_companion.presentation.viewmodel

import androidx.lifecycle.*
import com.example.travel_companion.data.local.entity.TripEntity
import com.example.travel_companion.data.repository.TripRepository
import com.example.travel_companion.domain.model.TripStatus
import com.example.travel_companion.service.TripMonitoringService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val tripRepository: TripRepository,
    private val tripMonitoringService: TripMonitoringService
) : ViewModel() {

    private val _currentDate = MutableLiveData<String>()
    val currentDate: LiveData<String> get() = _currentDate

    // LiveData del viaggio in corso
    private val currentTrip: LiveData<TripEntity?> =
        liveData(Dispatchers.IO) {
            emitSource(tripRepository.getTripsByStatus(TripStatus.STARTED))
        }.map { trips -> trips.firstOrNull() }

    // LiveData del prossimo viaggio programmato
    private val nextTrip: LiveData<TripEntity?> =
        liveData(Dispatchers.IO) {
            emitSource(tripRepository.getTripsByStatus(TripStatus.PLANNED))
        }.map { trips ->
            trips.filter { it.startDate > System.currentTimeMillis() }
                .minByOrNull { it.startDate }
        }

    // LiveData del tempo corrente, aggiornato ogni minuto
    private val nowLive = MutableLiveData(System.currentTimeMillis())

    // LiveData unico che la UI osserva
    val tripToShow = MediatorLiveData<TripEntity?>().apply {
        addSource(currentTrip) { current ->
            value = current ?: nextTrip.value
        }
        addSource(nextTrip) { next ->
            if (currentTrip.value == null) {
                value = next
            }
        }
        addSource(nowLive) {
            // Forza il ricalcolo dello stato
            value = currentTrip.value ?: nextTrip.value
        }
    }

    init {
        setupCurrentDate()
        startStatusMonitoring()
    }

    private fun setupCurrentDate() {
        val dateFormat = SimpleDateFormat("EEEE d MMMM yyyy", Locale.getDefault())
        _currentDate.value = dateFormat.format(Date())
    }

    private fun startStatusMonitoring() {
        tripMonitoringService.startMonitoring(viewModelScope)
    }

    override fun onCleared() {
        super.onCleared()
        tripMonitoringService.stopMonitoring()
    }
}