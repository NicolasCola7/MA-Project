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

    // Applied filters (currently active filters)
    private val _appliedStartDate = MutableLiveData<Long?>()
    private val _appliedEndDate = MutableLiveData<Long?>()
    private val _appliedDestination = MutableLiveData<String>()

    // Temporary filters (being edited in the overlay)
    private val _tempStartDate = MutableLiveData<Long?>()
    val tempStartDate: LiveData<Long?> get() = _tempStartDate

    private val _tempEndDate = MutableLiveData<Long?>()
    val tempEndDate: LiveData<Long?> get() = _tempEndDate

    private val _tempDestination = MutableLiveData<String>()
    val tempDestination: LiveData<String> get() = _tempDestination

    // UI events
    private val _filtersEvent = MutableLiveData<FiltersEvent>()
    val filtersEvent: LiveData<FiltersEvent> get() = _filtersEvent

    init {
        _appliedStartDate.value = null
        _appliedEndDate.value = null
        _appliedDestination.value = ""
        resetTempFilters()
    }

    /**
     * Sets the temporary start date filter.
     * @param date Date in milliseconds, or null to remove the filter.
     */
    fun setTempStartDate(date: Long?) {
        _tempStartDate.value = date
    }

    /**
     * Sets the temporary end date filter.
     * @param date Date in milliseconds, or null to remove the filter.
     */
    fun setTempEndDate(date: Long?) {
        _tempEndDate.value = date
    }

    /**
     * Sets the temporary destination filter.
     * @param destination Destination string.
     */
    fun setTempDestination(destination: String) {
        _tempDestination.value = destination
    }

    /**
     * Loads applied filters into temporary filters.
     * Useful for showing current values when opening the filter overlay.
     */
    fun loadAppliedFiltersToTemp() {
        _tempStartDate.value = _appliedStartDate.value
        _tempEndDate.value = _appliedEndDate.value
        _tempDestination.value = _appliedDestination.value
    }

    /**
     * Resets all temporary filters to null or empty values.
     */
    fun resetTempFilters() {
        _tempStartDate.value = null
        _tempEndDate.value = null
        _tempDestination.value = ""
    }

    /**
     * Applies temporary filters as the definitive applied filters.
     * Emits a FiltersApplied event with the updated values.
     */
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

    /**
     * Clears all applied and temporary filters.
     * Emits a FiltersCleared event to notify the UI.
     */
    fun clearAllFilters() {
        _appliedStartDate.value = null
        _appliedEndDate.value = null
        _appliedDestination.value = ""
        resetTempFilters()

        _filtersEvent.value = FiltersEvent.FiltersCleared
    }

    /**
     * Checks if there are any active filters.
     * @return true if at least one filter is applied, false otherwise.
     */
    fun hasActiveFilters(): Boolean {
        return _appliedStartDate.value != null ||
                _appliedEndDate.value != null ||
                !_appliedDestination.value.isNullOrEmpty()
    }

    /**
     * Filters a list of trips based on the applied filters.
     * @param allTrips Full list of TripEntity.
     * @return Filtered list of trips matching the criteria.
     */
    fun filterTrips(allTrips: List<TripEntity>): List<TripEntity> {
        val startDate = _appliedStartDate.value
        val endDate = _appliedEndDate.value
        val destination = _appliedDestination.value ?: ""

        return allTrips.filter { trip ->
            var includeTrip = true

            // Filter by start date
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

            // Filter by end date
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

            // Filter by destination (case-insensitive, startsWith match)
            if (destination.isNotBlank()) {
                includeTrip = includeTrip && trip.destination.lowercase().startsWith(destination.lowercase())
            }

            includeTrip
        }
    }

    /**
     * Filter events for notifying the UI.
     */
    sealed class FiltersEvent {
        /**
         * Event emitted when filters are applied.
         * @param startDate Applied start date, or null.
         * @param endDate Applied end date, or null.
         * @param destination Applied destination string.
         */
        data class FiltersApplied(
            val startDate: Long?,
            val endDate: Long?,
            val destination: String
        ) : FiltersEvent()

        /**
         * Event emitted when all filters are cleared.
         */
        object FiltersCleared : FiltersEvent()
    }
}
