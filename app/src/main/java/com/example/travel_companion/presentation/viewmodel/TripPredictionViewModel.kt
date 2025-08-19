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

    private val _suggestions = MutableStateFlow<List<String>>(emptyList())
    val suggestions: StateFlow<List<String>> = _suggestions.asStateFlow()

    private val _modelAccuracy = MutableStateFlow<Double?>(null)
    val modelAccuracy: StateFlow<Double?> = _modelAccuracy.asStateFlow()

    data class PredictionUiState(
        val isLoading: Boolean = false,
        val analysis: TripAnalysis? = null,
        val error: String? = null,
        val selectedTripType: String = "Tutti",
        val isModelLoading: Boolean = false,
        val lastRefresh: Long = 0L
    )

    init {
        loadPredictions()
        loadSuggestions()
        evaluateModelAccuracy()
    }

    fun loadPredictions() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                Timber.tag(TAG).d("Caricamento predizioni...")
                val analysis = predictionRepository.generateTripAnalysis()

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    analysis = analysis,
                    lastRefresh = System.currentTimeMillis()
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

    fun loadSuggestions() {
        viewModelScope.launch {
            try {
                Timber.tag(TAG).d("Caricamento suggerimenti personalizzati...")
                val personalizedSuggestions = predictionRepository.getPersonalizedSuggestions()
                _suggestions.value = personalizedSuggestions

                Timber.tag(TAG).d("Suggerimenti caricati: ${personalizedSuggestions.size}")

            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Errore nel caricamento dei suggerimenti")
                _suggestions.value = listOf("Impossibile caricare i suggerimenti al momento")
            }
        }
    }

    fun getPrimaryPrediction() {
        viewModelScope.launch {
            try {
                Timber.tag(TAG).d("Caricamento predizione primaria...")
                val primaryAnalysis = predictionRepository.getPrimaryPrediction()

                primaryAnalysis?.let { analysis ->
                    _uiState.value = _uiState.value.copy(analysis = analysis)
                    Timber.tag(TAG).d("Predizione primaria caricata")
                }

            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Errore nel caricamento della predizione primaria")
            }
        }
    }

    fun evaluateModelAccuracy() {
        viewModelScope.launch {
            try {
                Timber.tag(TAG).d("Valutazione accuratezza modello...")
                val accuracy = predictionRepository.evaluatePredictionAccuracy()
                _modelAccuracy.value = accuracy

                Timber.tag(TAG).d("Accuratezza modello: ${(accuracy * 100).toInt()}%")

            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Errore nella valutazione dell'accuratezza")
                _modelAccuracy.value = null
            }
        }
    }

    fun updateModel() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isModelLoading = true)

            try {
                Timber.tag(TAG).d("Aggiornamento modello...")
                val success = predictionRepository.updateModel()

                if (success) {
                    // Ricarica le predizioni con il modello aggiornato
                    loadPredictions()
                    evaluateModelAccuracy()

                    Timber.tag(TAG).d("Modello aggiornato con successo")
                } else {
                    _uiState.value = _uiState.value.copy(
                        error = "Impossibile aggiornare il modello al momento"
                    )
                }

            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Errore nell'aggiornamento del modello")
                _uiState.value = _uiState.value.copy(
                    error = "Errore nell'aggiornamento del modello: ${e.message}"
                )
            } finally {
                _uiState.value = _uiState.value.copy(isModelLoading = false)
            }
        }
    }

    fun getAdvancedStatistics(): Map<String, Any> {
        var stats = emptyMap<String, Any>()

        viewModelScope.launch {
            try {
                Timber.tag(TAG).d("Caricamento statistiche avanzate...")
                stats = predictionRepository.getAdvancedStatistics()

                Timber.tag(TAG).d("Statistiche caricate: ${stats.keys}")

            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Errore nel caricamento delle statistiche")
            }
        }

        return stats
    }

    fun refreshAll() {
        loadPredictions()
        loadSuggestions()
        evaluateModelAccuracy()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    override fun onCleared() {
        super.onCleared()
        // Cleanup del repository se necessario
        try {
            predictionRepository.cleanup()
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Errore durante la pulizia delle risorse")
        }
    }
}