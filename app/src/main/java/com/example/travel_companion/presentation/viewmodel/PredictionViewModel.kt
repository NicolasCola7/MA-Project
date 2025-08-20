package com.example.travel_companion.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travel_companion.data.repository.PredictionRepository
import com.example.travel_companion.domain.model.TravelPrediction
import com.example.travel_companion.domain.model.TravelSuggestion
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class PredictionViewModel @Inject constructor(
    private val predictionRepository: PredictionRepository
) : ViewModel() {

    private val _prediction = MutableStateFlow<TravelPrediction?>(null)
    val prediction: StateFlow<TravelPrediction?> = _prediction.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _suggestions = MutableStateFlow<List<TravelSuggestion>>(emptyList())

    companion object {
        private const val TAG = "PredictionViewModel"
    }

    init {
        loadPredictions()
        observePredictionChanges()
    }

    fun loadPredictions() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                // Carica previsioni
                val prediction = predictionRepository.calculateTravelPrediction()

                // Carica suggerimenti
                val suggestions = predictionRepository.getTravelSuggestions()

                _prediction.value = prediction
                _suggestions.value = suggestions
                _isLoading.value = false

                Timber.tag(TAG).d("Predizioni caricate: ${prediction.predictedTripsCount} viaggi, ${prediction.predictedDistance} km")

            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Errore nel caricamento predizioni")
                _isLoading.value = false
                _error.value = "Errore nel caricamento delle previsioni"
            }
        }
    }

    fun dismissError() {
        _error.value = null
    }

    private fun observePredictionChanges() {
        viewModelScope.launch {
            predictionRepository.getPredictionFlow()
                .catch { e ->
                    Timber.tag(TAG).e(e, "Errore nel flow predizioni")
                    _error.value = "Errore nell'aggiornamento delle previsioni"
                }
                .collect { prediction ->
                    _prediction.value = prediction
                }
        }
    }
}