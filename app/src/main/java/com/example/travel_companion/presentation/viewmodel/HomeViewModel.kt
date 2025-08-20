package com.example.travel_companion.presentation.viewmodel

import androidx.lifecycle.*
import com.example.travel_companion.data.local.entity.TripEntity
import com.example.travel_companion.data.repository.PredictionRepository
import com.example.travel_companion.data.repository.TripRepository
import com.example.travel_companion.domain.model.PredictionInsight
import com.example.travel_companion.domain.model.TravelSuggestion
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
    val currentDate: LiveData<String> get() = _currentDate

    // LiveData del viaggio in corso
    private val currentTrip: LiveData<TripEntity?> =
        tripRepository.getTripsByStatus(TripStatus.STARTED)
            .map { trips -> trips.firstOrNull() }

    // LiveData del prossimo viaggio programmato
    private val nextTrip: LiveData<TripEntity?> =
        tripRepository.getTripsByStatus(TripStatus.PLANNED)
            .map { trips ->
                trips.filter { it.startDate > System.currentTimeMillis() }
                    .minByOrNull { it.startDate }
            }

    // LiveData che la UI osserva
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

    // StateFlow per suggerimenti
    private val _suggestions = MutableStateFlow<List<TravelSuggestion>>(emptyList())
    val suggestions: StateFlow<List<TravelSuggestion>> = _suggestions.asStateFlow()

    // StateFlow per insights rapidi
    private val _insights = MutableStateFlow<List<PredictionInsight>>(emptyList())
    val insights: StateFlow<List<PredictionInsight>> = _insights.asStateFlow()

    // StateFlow per UI state
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    companion object {
        private const val TAG = "HomeViewModel"
        private const val MAX_HOME_SUGGESTIONS = 3
    }

    init {
        setupCurrentDate()
        loadSuggestions()
        observeTripChanges()
    }

    private fun setupCurrentDate() {
        val dateFormat = SimpleDateFormat("EEEE d MMMM yyyy", Locale.getDefault())
        _currentDate.value = dateFormat.format(Date())
    }

    private fun loadSuggestions() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoadingSuggestions = true)

                // Carica suggerimenti limitati per la home
                val allSuggestions = predictionRepository.getTravelSuggestions()
                val homeSuggestions = allSuggestions.take(MAX_HOME_SUGGESTIONS)

                // Carica insights rapidi solo se non ci sono viaggi attivi
                val shouldShowInsights = tripToShow.value == null
                val homeInsights = if (shouldShowInsights) {
                    predictionRepository.getPredictionInsights().take(2)
                } else {
                    emptyList()
                }

                _suggestions.value = homeSuggestions
                _insights.value = homeInsights

                _uiState.value = _uiState.value.copy(
                    isLoadingSuggestions = false,
                    showSuggestions = homeSuggestions.isNotEmpty(),
                    showInsights = homeInsights.isNotEmpty(),
                    error = null
                )

                Timber.tag(TAG).d("Caricati ${homeSuggestions.size} suggerimenti per home")

            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Errore nel caricamento suggerimenti")
                _uiState.value = _uiState.value.copy(
                    isLoadingSuggestions = false,
                    error = "Errore nel caricamento dei suggerimenti"
                )
            }
        }
    }

    private fun observeTripChanges() {
        // Ricarica suggerimenti quando cambia lo stato dei viaggi
        tripToShow.observeForever { trip ->
            viewModelScope.launch {
                loadSuggestions()
            }
        }
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun onSuggestionClicked(suggestion: TravelSuggestion) {
        // Analytics o tracking del click
        Timber.tag(TAG).d("Suggestion clicked: ${suggestion.destination}")

        // Potresti emettere un evento per navigare verso la creazione del viaggio
        // con i dati pre-compilati del suggerimento
    }

    fun onViewAllSuggestionsClicked() {
        // Naviga verso la sezione completa dei suggerimenti (probabilmente nelle statistiche)
        Timber.tag(TAG).d("View all suggestions clicked")
    }

    fun onInsightActionClicked(insight: PredictionInsight) {
        // Gestisce le azioni degli insights
        when (insight.actionType) {
            com.example.travel_companion.domain.model.ActionType.PLAN_TRIP -> {
                // Naviga verso pianificazione viaggio
                Timber.tag(TAG).d("Navigate to trip planning")
            }
            com.example.travel_companion.domain.model.ActionType.VIEW_SUGGESTIONS -> {
                // Mostra tutti i suggerimenti
                onViewAllSuggestionsClicked()
            }
            com.example.travel_companion.domain.model.ActionType.VIEW_STATISTICS -> {
                // Naviga verso statistiche
                Timber.tag(TAG).d("Navigate to statistics")
            }
            null -> {
                // Nessuna azione
            }
        }
    }

    data class HomeUiState(
        val isLoadingSuggestions: Boolean = false,
        val showSuggestions: Boolean = false,
        val showInsights: Boolean = false,
        val error: String? = null
    )
}