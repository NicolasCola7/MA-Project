package com.example.travel_companion.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travel_companion.data.repository.TripPredictionRepository
import com.example.travel_companion.domain.model.TripAnalysis
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class TripPredictionViewModel @Inject constructor(
    private val predictionRepository: TripPredictionRepository
) : ViewModel() {

    companion object {
        private const val TAG = "TripPredictionVM"
    }

    private val _uiState = MutableStateFlow(PredictionUiState())
    val uiState: StateFlow<PredictionUiState> = _uiState.asStateFlow()

    data class PredictionUiState(
        val isLoading: Boolean = false,
        val analysis: TripAnalysis? = null,
        val error: String? = null,
        val selectedTripType: String = "Tutti"
    )

    init {
        loadPredictions()
    }

    fun loadPredictions() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                Timber.tag(TAG).d("Caricamento predizioni...")
                val analysis = predictionRepository.generateTripAnalysis()

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    analysis = analysis
                )

                Timber.tag(TAG).d("Predizioni caricate: ${analysis.tripPredictions.size}")

            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Errore nel caricamento delle predizioni")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Errore nel caricamento delle previsioni: ${e.message}"
                )
            }
        }
    }

    fun filterByTripType(tripType: String) {
        if (tripType == _uiState.value.selectedTripType) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, selectedTripType = tripType)

            try {
                Timber.tag(TAG).d("Filtraggio per tipo: $tripType")

                val analysis = if (tripType == "Tutti") {
                    predictionRepository.generateTripAnalysis()
                } else {
                    predictionRepository.getPredictionsForTripType(tripType)
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    analysis = analysis
                )

                Timber.tag(TAG).d("Filtraggio completato per '$tripType'")

            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Errore nel filtraggio")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Errore nel filtraggio: ${e.message}"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    override fun onCleared() {
        super.onCleared()
        try {
            predictionRepository.cleanup()
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Errore durante la pulizia delle risorse")
        }
    }
}