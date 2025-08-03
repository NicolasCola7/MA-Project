package com.example.travel_companion.presentation.viewmodel

import androidx.lifecycle.*
import com.example.travel_companion.data.local.entity.TripEntity
import com.example.travel_companion.data.repository.TripRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val tripRepository: TripRepository
) : ViewModel() {

    private val _currentDate = MutableLiveData<String>()
    val currentDate: LiveData<String> get() = _currentDate

    // LiveData del viaggio in corso
    private val currentTrip: LiveData<TripEntity?> =
        liveData(Dispatchers.IO) {
            emitSource(tripRepository.getTripAtTimeLive(System.currentTimeMillis()))
        }

    // LiveData del prossimo viaggio programmato
    private val nextTrip: LiveData<TripEntity?> =
        liveData(Dispatchers.IO) {
            emitSource(tripRepository.getNextPlannedTripLive(System.currentTimeMillis()))
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
            // Forza il ricalcolo dello stato senza nuova query se necessario
            value = currentTrip.value ?: nextTrip.value
        }
    }

    init {
        // Aggiorna la data odierna
        val dateFormat = SimpleDateFormat("EEEE d MMMM yyyy", Locale.getDefault())
        _currentDate.value = dateFormat.format(Date())

        // Aggiorna "ora" ogni minuto
        viewModelScope.launch {
            while (true) {
                delay(60_000) // ogni minuto
                nowLive.postValue(System.currentTimeMillis())
            }
        }
    }
}
