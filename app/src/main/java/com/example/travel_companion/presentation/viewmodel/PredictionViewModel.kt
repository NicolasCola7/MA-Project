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

    private val _uiState = MutableStateFlow(PredictionUiState())
    val uiState: StateFlow<PredictionUiState> = _uiState.asStateFlow()

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
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                // Carica previsioni
                val prediction = predictionRepository.calculateTravelPrediction()

                // Carica suggerimenti
                val suggestions = predictionRepository.getTravelSuggestions()

                _uiState.value = _uiState.value.copy(
                    prediction = prediction,
                    isLoading = false
                )
                _suggestions.value = suggestions

                Timber.tag(TAG).d("Predizioni caricate: ${prediction.predictedTripsCount} viaggi, ${prediction.predictedDistance} km")

            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Errore nel caricamento predizioni")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Errore nel caricamento delle previsioni"
                )
            }
        }
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private fun observePredictionChanges() {
        viewModelScope.launch {
            predictionRepository.getPredictionFlow()
                .catch { e ->
                    Timber.tag(TAG).e(e, "Errore nel flow predizioni")
                }
                .collect { prediction ->
                    _uiState.value = _uiState.value.copy(prediction = prediction)
                }
        }
    }

    data class PredictionUiState(
        val prediction: TravelPrediction? = null,
        val isLoading: Boolean = false,
        val error: String? = null
    )
}