package com.example.travel_companion.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travel_companion.data.repository.PredictionRepository
import com.example.travel_companion.domain.model.TripPrediction
import com.example.travel_companion.domain.model.TripSuggestion
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class PredictionViewModel @Inject constructor(
    private val predictionRepository: PredictionRepository
) : ViewModel() {

    private val _prediction = MutableStateFlow<TripPrediction?>(null)
    val prediction: StateFlow<TripPrediction?> = _prediction.asStateFlow()

    private val _suggestions = MutableStateFlow<List<TripSuggestion>>(emptyList())

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
                // Carica previsioni
                val prediction = predictionRepository.calculateTravelPrediction()

                // Carica suggerimenti
                val suggestions = predictionRepository.getTravelSuggestions()

                _prediction.value = prediction
                _suggestions.value = suggestions
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Errore nel caricamento predizioni")
            }
        }
    }

    private fun observePredictionChanges() {
        viewModelScope.launch {
            predictionRepository.getPredictionFlow()
                .catch { e ->
                    Timber.tag(TAG).e(e, "Errore nel flow predizioni")
                }
                .collect { prediction ->
                    _prediction.value = prediction
                }
        }
    }
}