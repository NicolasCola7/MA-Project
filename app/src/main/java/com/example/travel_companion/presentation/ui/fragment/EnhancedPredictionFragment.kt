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
import androidx.recyclerview.widget.RecyclerView
import com.example.travel_companion.databinding.FragmentPredictionBinding // CAMBIATO QUI
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
class EnhancedPredictionFragment : Fragment() {

    private var _binding: FragmentPredictionBinding? = null // CAMBIATO QUI
    private val binding get() = _binding!!

    private val viewModel: TripPredictionViewModel by viewModels()

    private lateinit var tripPredictionAdapter: TripPredictionAdapter
    private lateinit var suggestionAdapter: SuggestionAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPredictionBinding.inflate(inflater, container, false) // CAMBIATO QUI
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerViews()
        setupChips()
        setupClickListeners()
        observeViewModel()
        setupSwipeRefresh()
    }

    private fun setupRecyclerViews() {
        // Setup predictions RecyclerView
        tripPredictionAdapter = TripPredictionAdapter { prediction ->
            Timber.tag("PredictionFragment")
                .d("Click su predizione: ${prediction.suggestedDestination}")
            showTripPredictionDialog(prediction)
        }

        binding.recyclerViewTripPredictions.apply {
            adapter = tripPredictionAdapter
            layoutManager = LinearLayoutManager(context)
        }

        // Setup suggestions RecyclerView (solo se esiste nel layout)
        try {
            suggestionAdapter = SuggestionAdapter { suggestion ->
                Timber.tag("PredictionFragment")
                    .d("Click su suggerimento: $suggestion")
                showSuggestionDialog(suggestion)
            }

            // Cerca di trovare la RecyclerView per i suggerimenti
            // Se non esiste, crea una vista temporanea
            setupSuggestionsIfExists()
        } catch (e: Exception) {
            Timber.tag("PredictionFragment").w("RecyclerView suggerimenti non trovata: ${e.message}")
        }
    }

    private fun setupSuggestionsIfExists() {
        // Controlla se esiste la RecyclerView per suggerimenti
        val suggestionsView = binding.root.findViewById<RecyclerView?>(
            binding.root.context.resources.getIdentifier(
                "recyclerViewSuggestions",
                "id",
                binding.root.context.packageName
            )
        )

        suggestionsView?.apply {
            adapter = suggestionAdapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
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

    private fun setupClickListeners() {
        // Solo i bottoni che esistono nel layout originale
        try {
            // Cerca bottoni opzionali
            val buttonUpdateModel = binding.root.findViewById<View?>(
                binding.root.context.resources.getIdentifier(
                    "buttonUpdateModel",
                    "id",
                    binding.root.context.packageName
                )
            )

            buttonUpdateModel?.setOnClickListener {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Aggiorna Modello AI")
                    .setMessage("Vuoi aggiornare il modello di predizione con gli ultimi dati?")
                    .setPositiveButton("Aggiorna") { _, _ ->
                        viewModel.updateModel()
                    }
                    .setNegativeButton("Annulla", null)
                    .show()
            }

            // Altri bottoni opzionali...
            setupOptionalButtons()

        } catch (e: Exception) {
            Timber.tag("PredictionFragment").w("Alcuni bottoni non trovati: ${e.message}")
        }
    }

    private fun setupOptionalButtons() {
        // Bottone statistiche avanzate (opzionale)
        val buttonAdvancedStats = binding.root.findViewById<View?>(
            binding.root.context.resources.getIdentifier(
                "buttonAdvancedStats",
                "id",
                binding.root.context.packageName
            )
        )

        buttonAdvancedStats?.setOnClickListener {
            showAdvancedStatistics()
        }

        // Bottone predizione primaria (opzionale)
        val buttonPrimaryPrediction = binding.root.findViewById<View?>(
            binding.root.context.resources.getIdentifier(
                "buttonPrimaryPrediction",
                "id",
                binding.root.context.packageName
            )
        )

        buttonPrimaryPrediction?.setOnClickListener {
            viewModel.getPrimaryPrediction()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                updateUI(state)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.suggestions.collect { suggestions ->
                updateSuggestions(suggestions)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.modelAccuracy.collect { accuracy ->
                updateModelAccuracy(accuracy)
            }
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.refreshAll()
        }
    }

    private fun updateUI(state: TripPredictionViewModel.PredictionUiState) {
        binding.swipeRefreshLayout.isRefreshing = state.isLoading
        binding.progressBar.isVisible = state.isLoading && state.analysis == null

        // Mostra/nascondi loading del modello (se esiste)
        val progressModelUpdate = binding.root.findViewById<View?>(
            binding.root.context.resources.getIdentifier(
                "progressModelUpdate",
                "id",
                binding.root.context.packageName
            )
        )
        progressModelUpdate?.isVisible = state.isModelLoading

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

        // Aggiorna timestamp ultimo refresh (se esiste)
        updateLastRefreshTime(state.lastRefresh)
    }

    private fun updateLastRefreshTime(lastRefresh: Long) {
        if (lastRefresh > 0) {
            val textLastUpdate = binding.root.findViewById<android.widget.TextView?>(
                binding.root.context.resources.getIdentifier(
                    "textLastUpdate",
                    "id",
                    binding.root.context.packageName
                )
            )

            textLastUpdate?.let {
                val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                it.text = "Ultimo aggiornamento: ${dateFormat.format(Date(lastRefresh))}"
                it.isVisible = true
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

        // Forza layout
        binding.recyclerViewTripPredictions.post {
            binding.recyclerViewTripPredictions.requestLayout()
        }

        binding.textNoPredictions.isVisible = analysis.tripPredictions.isEmpty()

        // Aggiorna contatore predizioni (se esiste)
        val textPredictionsCount = binding.root.findViewById<android.widget.TextView?>(
            binding.root.context.resources.getIdentifier(
                "textPredictionsCount",
                "id",
                binding.root.context.packageName
            )
        )

        textPredictionsCount?.let {
            it.text = "${analysis.tripPredictions.size} predizioni"
            it.isVisible = analysis.tripPredictions.isNotEmpty()
        }
    }

    private fun updateSuggestions(suggestions: List<String>) {
        Timber.tag("PredictionFragment").d("Aggiornamento ${suggestions.size} suggerimenti")

        if (::suggestionAdapter.isInitialized) {
            suggestionAdapter.submitList(suggestions)

            // Aggiorna layout suggerimenti (se esiste)
            val layoutSuggestions = binding.root.findViewById<View?>(
                binding.root.context.resources.getIdentifier(
                    "layoutSuggestions",
                    "id",
                    binding.root.context.packageName
                )
            )
            layoutSuggestions?.isVisible = suggestions.isNotEmpty()
        }
    }

    private fun updateModelAccuracy(accuracy: Double?) {
        val textModelAccuracy = binding.root.findViewById<android.widget.TextView?>(
            binding.root.context.resources.getIdentifier(
                "textModelAccuracy",
                "id",
                binding.root.context.packageName
            )
        )

        textModelAccuracy?.let { textView ->
            accuracy?.let {
                val percentage = (it * 100).toInt()
                textView.text = "Accuratezza modello: $percentage%"
                textView.isVisible = true

                // Cambia colore basato sull'accuratezza
                val color = when {
                    percentage >= 80 -> android.R.color.holo_green_dark
                    percentage >= 60 -> android.R.color.holo_orange_dark
                    else -> android.R.color.holo_red_dark
                }
                textView.setTextColor(ContextCompat.getColor(requireContext(), color))
            } ?: run {
                textView.isVisible = false
            }
        }
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

    private fun showSuggestionDialog(suggestion: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Suggerimento Personalizzato")
            .setMessage(suggestion)
            .setPositiveButton("Interessante!", null)
            .setNegativeButton("Non ora", null)
            .show()
    }

    private fun showAdvancedStatistics() {
        val stats = viewModel.getAdvancedStatistics()

        val statsText = buildString {
            stats.forEach { (key, value) ->
                appendLine("$key: $value")
            }
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Statistiche Avanzate")
            .setMessage(if (statsText.isNotEmpty()) statsText else "Caricamento statistiche...")
            .setPositiveButton("OK", null)
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

// Adapter per i suggerimenti (semplificato)
class SuggestionAdapter(
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<SuggestionAdapter.ViewHolder>() {

    private var suggestions = listOf<String>()

    fun submitList(newSuggestions: List<String>) {
        suggestions = newSuggestions
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(suggestions[position])
    }

    override fun getItemCount() = suggestions.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(suggestion: String) {
            (itemView as android.widget.TextView).apply {
                text = suggestion
                setOnClickListener { onItemClick(suggestion) }
                setPadding(32, 16, 32, 16)
            }
        }
    }
}