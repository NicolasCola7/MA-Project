package com.example.travel_companion.presentation.viewmodel

import androidx.lifecycle.*
import com.example.travel_companion.data.local.entity.TripEntity
import com.example.travel_companion.data.repository.PredictionRepository
import com.example.travel_companion.data.repository.TripRepository
import com.example.travel_companion.domain.model.TripSuggestion
import com.example.travel_companion.domain.model.TripStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    tripRepository: TripRepository,
    private val predictionRepository: PredictionRepository
) : ViewModel() {

    private val _currentDate = MutableLiveData<String>()

    // LiveData for the currently active trip
    private val currentTrip: LiveData<TripEntity?> =
        tripRepository.getTripsByStatus(TripStatus.STARTED)
            .map { trips -> trips.firstOrNull() }

    // LiveData for the next scheduled trip
    private val nextTrip: LiveData<TripEntity?> =
        tripRepository.getTripsByStatus(TripStatus.PLANNED)
            .map { trips ->
                trips.filter { it.startDate > System.currentTimeMillis() }
                    .minByOrNull { it.startDate }
            }

    // LiveData observed by the UI
    val tripToShow = MediatorLiveData<TripEntity?>().apply {
        addSource(currentTrip) { current ->
            value = current ?: nextTrip.value
        }
        addSource(nextTrip) { next ->
            if (currentTrip.value == null) {
                value = next
            }
        }
    }

    // StateFlow for trip suggestions
    private val _suggestions = MutableStateFlow<List<TripSuggestion>>(emptyList())
    val suggestions: StateFlow<List<TripSuggestion>> = _suggestions.asStateFlow()

    private val _showSuggestions = MutableStateFlow(false)
    val showSuggestions: StateFlow<Boolean> = _showSuggestions.asStateFlow()

    companion object {
        private const val TAG = "HomeViewModel"
        private const val MAX_HOME_SUGGESTIONS = 3
    }

    init {
        setupCurrentDate()
        loadSuggestions()
        observeTripChanges()
    }

    /**
     * Sets up the current date in a human-readable format.
     */
    private fun setupCurrentDate() {
        val dateFormat = SimpleDateFormat("EEEE d MMMM yyyy", Locale.getDefault())
        _currentDate.value = dateFormat.format(Date())
    }

    /**
     * Loads trip suggestions for the home screen.
     * Limits the number of suggestions to MAX_HOME_SUGGESTIONS.
     */
    private fun loadSuggestions() {
        viewModelScope.launch {
            try {
                val allSuggestions = predictionRepository.getTravelSuggestions()
                val homeSuggestions = allSuggestions.take(MAX_HOME_SUGGESTIONS)

                _suggestions.value = homeSuggestions
                _showSuggestions.value = homeSuggestions.isNotEmpty()
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Error loading suggestions")
            }
        }
    }

    /**
     * Observes changes in trips and reloads suggestions when the tripToShow changes.
     */
    private fun observeTripChanges() {
        tripToShow.observeForever {
            viewModelScope.launch {
                loadSuggestions()
            }
        }
    }
}
