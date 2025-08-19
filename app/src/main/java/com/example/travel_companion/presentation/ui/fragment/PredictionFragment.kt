package com.example.travel_companion.presentation.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.travel_companion.databinding.FragmentPredictionBinding
import com.example.travel_companion.domain.model.TripAnalysis
import com.example.travel_companion.presentation.adapter.TripPredictionAdapter
import com.example.travel_companion.presentation.viewmodel.TripPredictionViewModel
import com.example.travel_companion.domain.model.TripPrediction
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class PredictionFragment : Fragment() {

    private var _binding: FragmentPredictionBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TripPredictionViewModel by viewModels()
    private lateinit var tripPredictionAdapter: TripPredictionAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPredictionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupChips()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        tripPredictionAdapter = TripPredictionAdapter { prediction ->
            Timber.tag("PredictionFragment")
                .d("Click su predizione: ${prediction.suggestedDestination}")
            showTripPredictionDialog(prediction)
        }

        binding.recyclerViewTripPredictions.apply {
            adapter = tripPredictionAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }

    private fun setupChips() {
        val tripTypes = listOf("Tutti", "Viaggio locale", "Gita giornaliera", "Viaggio di più giorni")

        tripTypes.forEach { type ->
            val chip = Chip(context).apply {
                text = type
                isCheckable = true
                isChecked = type == "Tutti"
                setOnClickListener {
                    // Deseleziona altri chip
                    for (i in 0 until binding.chipGroupTripTypes.childCount) {
                        val otherChip = binding.chipGroupTripTypes.getChildAt(i) as Chip
                        otherChip.isChecked = otherChip == this
                    }
                    viewModel.filterByTripType(type)
                }
            }
            binding.chipGroupTripTypes.addView(chip)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                updateUI(state)
            }
        }
    }

    private fun updateUI(state: TripPredictionViewModel.PredictionUiState) {
        // Mostra/nascondi progress bar
        binding.progressBar.isVisible = state.isLoading

        // Gestisci errori
        state.error?.let { error ->
            Snackbar.make(binding.root, error, Snackbar.LENGTH_LONG)
                .setAction("Riprova") { viewModel.loadPredictions() }
                .show()
            viewModel.clearError()
        }

        // Aggiorna contenuto
        state.analysis?.let { analysis ->
            updateStatistics(analysis)
            updatePredictions(analysis)

            binding.layoutContent.isVisible = true
            binding.layoutEmpty.isVisible = false
        } ?: run {
            if (!state.isLoading) {
                binding.layoutContent.isVisible = false
                binding.layoutEmpty.isVisible = true
            }
        }
    }

    private fun updateStatistics(analysis: TripAnalysis) {
        with(binding) {
            textTotalTrips.text = analysis.totalTrips.toString()
            textAvgTripsPerMonth.text = String.format("%.1f", analysis.averageTripsPerMonth)
            textFavoriteType.text = analysis.favoriteDestinationType
            textAvgDuration.text = String.format("%.1f giorni", analysis.averageTripDuration)
            textAvgDistance.text = String.format("%.1f km", analysis.averageDistancePerTrip)

            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            textNextPredictedTrip.text = dateFormat.format(Date(analysis.nextPredictedTripDate))
        }
    }

    private fun updatePredictions(analysis: TripAnalysis) {
        Timber.tag("PredictionFragment").d("=== UPDATE PREDICTIONS ===")
        Timber.tag("PredictionFragment")
            .d("Numero previsioni ricevute: ${analysis.tripPredictions.size}")

        tripPredictionAdapter.submitList(analysis.tripPredictions)
        binding.textNoPredictions.isVisible = analysis.tripPredictions.isEmpty()
    }

    private fun showTripPredictionDialog(prediction: TripPrediction) {
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Dettagli Predizione")
            .setMessage("""
                Destinazione: ${prediction.suggestedDestination}
                Tipo viaggio: ${prediction.predictedType}
                Data inizio: ${dateFormat.format(Date(prediction.suggestedStartDate))}
                Data fine: ${dateFormat.format(Date(prediction.suggestedEndDate))}
                Confidenza: ${(prediction.confidence * 100).toInt()}%
                
                Motivo: ${prediction.reasoning}
            """.trimIndent())
            .setPositiveButton("Pianifica Viaggio") { _, _ ->
                planTripFromPrediction(prediction)
            }
            .setNegativeButton("Chiudi", null)
            .show()
    }

    private fun planTripFromPrediction(prediction: TripPrediction) {
        Timber.tag("PredictionFragment")
            .d("Pianificazione viaggio da predizione: ${prediction.suggestedDestination}")

        Snackbar.make(
            binding.root,
            "Funzione di pianificazione viaggio in sviluppo",
            Snackbar.LENGTH_SHORT
        ).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}