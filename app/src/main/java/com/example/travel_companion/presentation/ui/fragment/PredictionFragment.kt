// File: presentation/ui/fragment/PredictionFragment.kt - VERSIONE CORRETTA
package com.example.travel_companion.presentation.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.travel_companion.R
import com.example.travel_companion.databinding.FragmentPredictionBinding
import com.example.travel_companion.presentation.adapter.TripPredictionAdapter
import com.example.travel_companion.presentation.adapter.POISuggestionAdapter
import com.example.travel_companion.presentation.viewmodel.TravelPredictionViewModel
import com.example.travel_companion.domain.model.TripPrediction
import com.example.travel_companion.domain.model.POISuggestion
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class PredictionFragment : Fragment() {

    private var _binding: FragmentPredictionBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TravelPredictionViewModel by viewModels()

    private lateinit var tripPredictionAdapter: TripPredictionAdapter
    private lateinit var poiSuggestionAdapter: POISuggestionAdapter

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

        setupRecyclerViews()
        setupChips()
        observeViewModel()
        setupSwipeRefresh()
    }

    private fun setupRecyclerViews() {
        tripPredictionAdapter = TripPredictionAdapter { prediction ->
            showTripPredictionDialog(prediction)
        }

        poiSuggestionAdapter = POISuggestionAdapter { poiSuggestion ->
            showPOIDialog(poiSuggestion)
        }

        // ID corretti dal layout
        binding.recyclerViewTripPredictions.apply {
            adapter = tripPredictionAdapter
            layoutManager = LinearLayoutManager(context)
        }

        binding.recyclerViewPoiSuggestions.apply {
            adapter = poiSuggestionAdapter
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

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.loadPredictions()
        }
    }

    private fun updateUI(state: TravelPredictionViewModel.PredictionUiState) {
        binding.swipeRefreshLayout.isRefreshing = state.isLoading
        binding.progressBar.isVisible = state.isLoading && state.analysis == null

        state.error?.let { error ->
            Snackbar.make(binding.root, error, Snackbar.LENGTH_LONG)
                .setAction("Riprova") { viewModel.loadPredictions() }
                .show()
            viewModel.clearError()
        }

        state.analysis?.let { analysis ->
            updateStatistics(analysis)
            updatePredictions(analysis)
            updatePOISuggestions(analysis)

            binding.layoutContent.isVisible = true
            binding.layoutEmpty.isVisible = false
        } ?: run {
            if (!state.isLoading) {
                binding.layoutContent.isVisible = false
                binding.layoutEmpty.isVisible = true
            }
        }
    }

    private fun updateStatistics(analysis: com.example.travel_companion.domain.model.TravelAnalysis) {
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

    private fun updatePredictions(analysis: com.example.travel_companion.domain.model.TravelAnalysis) {
        tripPredictionAdapter.submitList(analysis.tripPredictions)
        binding.textNoPredictions.isVisible = analysis.tripPredictions.isEmpty()
    }

    private fun updatePOISuggestions(analysis: com.example.travel_companion.domain.model.TravelAnalysis) {
        poiSuggestionAdapter.submitList(analysis.poiSuggestions)
        binding.textNoPoiSuggestions.isVisible = analysis.poiSuggestions.isEmpty()
    }

    private fun showTripPredictionDialog(prediction: TripPrediction) {
        // TODO: Implementa dialog per mostrare dettagli predizione
        // Potresti navigare ad AddTripFragment con dati pre-compilati
    }

    private fun showPOIDialog(poiSuggestion: POISuggestion) {
        // TODO: Implementa dialog per mostrare dettagli POI
        // Potresti aprire una mappa o salvare direttamente il POI
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}