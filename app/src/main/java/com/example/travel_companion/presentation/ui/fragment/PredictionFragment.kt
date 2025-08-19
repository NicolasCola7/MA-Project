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
import androidx.recyclerview.widget.RecyclerView
import com.example.travel_companion.databinding.FragmentPredictionBinding
import com.example.travel_companion.domain.model.TripAnalysis
import com.example.travel_companion.presentation.adapter.TripPredictionAdapter
import com.example.travel_companion.presentation.viewmodel.TripPredictionViewModel
import com.example.travel_companion.domain.model.TripPrediction
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
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

        setupRecyclerViews()
        setupChips()
        observeViewModel()
        setupSwipeRefresh()
    }

    private fun setupRecyclerViews() {
        tripPredictionAdapter = TripPredictionAdapter { prediction ->
            Timber.tag("PredictionFragment")
                .d("Click su predizione: ${prediction.suggestedDestination}")
            showTripPredictionDialog(prediction)
        }

        // Aggiungi un listener per verificare quando l'adapter riceve i dati
        tripPredictionAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                Timber.tag("PredictionFragment")
                    .d("Adapter dati cambiati - item count: ${tripPredictionAdapter.itemCount}")
            }

            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                Timber.tag("PredictionFragment")
                    .d("Items inseriti: $itemCount dalla posizione $positionStart")
            }
        })

        tripPredictionAdapter = TripPredictionAdapter { prediction ->
            showTripPredictionDialog(prediction)
        }

        // ID corretti dal layout
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

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.loadPredictions()
        }
    }

    private fun updateUI(state: TripPredictionViewModel.PredictionUiState) {
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

        analysis.tripPredictions.forEachIndexed { index, prediction ->
            Timber.tag("PredictionFragment").d("UI Predizione $index:")
            Timber.tag("PredictionFragment")
                .d("  - Destinazione: ${prediction.suggestedDestination}")
            Timber.tag("PredictionFragment")
                .d("  - Confidenza: ${(prediction.confidence * 100).toInt()}%")
            Timber.tag("PredictionFragment").d("  - Tipo: ${prediction.predictedType}")
        }

        tripPredictionAdapter.submitList(analysis.tripPredictions)

        // AGGIUNGI QUESTO per forzare il layout:
        binding.recyclerViewTripPredictions.post {
            binding.recyclerViewTripPredictions.requestLayout()
            Timber.tag("PredictionFragment")
                .d("Layout forzato - altezza RecyclerView: ${binding.recyclerViewTripPredictions.height}")
        }

        binding.textNoPredictions.isVisible = analysis.tripPredictions.isEmpty()

        Timber.tag("PredictionFragment").d("Lista sottomessa all'adapter")
        Timber.tag("PredictionFragment")
            .d("textNoPredictions visibility: ${binding.textNoPredictions.isVisible}")
    }

    private fun showTripPredictionDialog(prediction: TripPrediction) {
        // TODO: Implementa dialog per mostrare dettagli predizione
        // Potresti navigare ad AddTripFragment con dati pre-compilati
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}