package com.example.travel_companion.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.travel_companion.data.local.entity.TripEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.*
import javax.inject.Inject

@HiltViewModel
class FiltersViewModel @Inject constructor() : ViewModel() {

    // Filtri applicati (quelli effettivamente in uso)
    private val _appliedStartDate = MutableLiveData<Long?>()
    private val _appliedEndDate = MutableLiveData<Long?>()
    private val _appliedDestination = MutableLiveData<String>()

    // Filtri temporanei (in fase di modifica nell'overlay)
    private val _tempStartDate = MutableLiveData<Long?>()
    val tempStartDate: LiveData<Long?> get() = _tempStartDate

    private val _tempEndDate = MutableLiveData<Long?>()
    val tempEndDate: LiveData<Long?> get() = _tempEndDate

    private val _tempDestination = MutableLiveData<String>()
    val tempDestination: LiveData<String> get() = _tempDestination

    // Eventi per la UI
    private val _filtersEvent = MutableLiveData<FiltersEvent>()
    val filtersEvent: LiveData<FiltersEvent> get() = _filtersEvent

    init {
        _appliedStartDate.value = null
        _appliedEndDate.value = null
        _appliedDestination.value = ""
        resetTempFilters()
    }

    // Gestione filtri temporanei
    fun setTempStartDate(date: Long?) {
        _tempStartDate.value = date
    }

    fun setTempEndDate(date: Long?) {
        _tempEndDate.value = date
    }

    fun setTempDestination(destination: String) {
        _tempDestination.value = destination
    }

    // Carica i filtri applicati nei temporanei (quando si apre l'overlay)
    fun loadAppliedFiltersToTemp() {
        _tempStartDate.value = _appliedStartDate.value
        _tempEndDate.value = _appliedEndDate.value
        _tempDestination.value = _appliedDestination.value
    }

    // Reset dei filtri temporanei
    fun resetTempFilters() {
        _tempStartDate.value = null
        _tempEndDate.value = null
        _tempDestination.value = ""
    }

    // Applica i filtri temporanei come definitivi
    fun applyFilters() {
        _appliedStartDate.value = _tempStartDate.value
        _appliedEndDate.value = _tempEndDate.value
        _appliedDestination.value = _tempDestination.value

        _filtersEvent.value = FiltersEvent.FiltersApplied(
            startDate = _appliedStartDate.value,
            endDate = _appliedEndDate.value,
            destination = _appliedDestination.value ?: ""
        )
    }

    // Pulisce tutti i filtri
    fun clearAllFilters() {
        _appliedStartDate.value = null
        _appliedEndDate.value = null
        _appliedDestination.value = ""
        resetTempFilters()

        _filtersEvent.value = FiltersEvent.FiltersCleared
    }

    // Verifica se ci sono filtri attivi
    fun hasActiveFilters(): Boolean {
        return _appliedStartDate.value != null ||
                _appliedEndDate.value != null ||
                !_appliedDestination.value.isNullOrEmpty()
    }

    // Metodo per filtrare i viaggi
    fun filterTrips(allTrips: List<TripEntity>): List<TripEntity> {
        val startDate = _appliedStartDate.value
        val endDate = _appliedEndDate.value
        val destination = _appliedDestination.value ?: ""

        return allTrips.filter { trip ->
            var includeTrip = true

            // Filtro per data inizio
            if (startDate != null) {
                val tripStartDate = Calendar.getInstance().apply {
                    timeInMillis = trip.startDate
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val filterStartDate = Calendar.getInstance().apply {
                    timeInMillis = startDate
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                includeTrip = tripStartDate.timeInMillis >= filterStartDate.timeInMillis
            }

            // Filtro per data fine
            if (endDate != null) {
                val tripStartDate = Calendar.getInstance().apply {
                    timeInMillis = trip.startDate
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val filterEndDate = Calendar.getInstance().apply {
                    timeInMillis = endDate
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                includeTrip = includeTrip && tripStartDate.timeInMillis <= filterEndDate.timeInMillis
            }

            // Filtro per destinazione (case-insensitive, startsWith match)
            if (destination.isNotBlank()) {
                includeTrip = includeTrip && trip.destination.lowercase().startsWith(destination.lowercase())
            }

            includeTrip
        }
    }

    sealed class FiltersEvent {
        data class FiltersApplied(
            val startDate: Long?,
            val endDate: Long?,
            val destination: String
        ) : FiltersEvent()

        object FiltersCleared : FiltersEvent()
    }
}