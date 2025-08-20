package com.example.travel_companion.presentation.ui.fragment

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.travel_companion.R
import com.example.travel_companion.databinding.FragmentHomeBinding
import com.example.travel_companion.domain.model.TripStatus
import com.example.travel_companion.presentation.ui.adapter.HomeSuggestionsAdapter
import com.example.travel_companion.presentation.ui.adapter.InsightsAdapter
import com.example.travel_companion.presentation.viewmodel.HomeViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HomeViewModel by viewModels()

    private lateinit var suggestionsAdapter: HomeSuggestionsAdapter
    private lateinit var insightsAdapter: InsightsAdapter

    companion object {
        private const val TAG = "HomeFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(inflater, R.layout.fragment_home, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerViews()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupRecyclerViews() {
        // Setup Suggestions RecyclerView
        suggestionsAdapter = HomeSuggestionsAdapter { suggestion ->
            viewModel.onSuggestionClicked(suggestion)

            // Mostra Snackbar con azione per pianificare
            Snackbar.make(
                binding.root,
                "Pianifica viaggio a ${suggestion.destination}?",
                Snackbar.LENGTH_LONG
            ).setAction("Pianifica") {
                // TODO: Naviga verso AddTripFragment con dati pre-compilati
                // val bundle = bundleOf(
                //     "destination" to suggestion.destination,
                //     "type" to suggestion.type,
                //     "estimated_distance" to suggestion.estimatedDistance
                // )
                // findNavController().navigate(R.id.action_home_to_addTrip, bundle)

                Timber.tag(TAG).d("Navigating to trip planning for ${suggestion.destination}")
            }.show()
        }

        binding.suggestionsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = suggestionsAdapter
        }

        // Setup Insights RecyclerView
        insightsAdapter = InsightsAdapter { insight ->
            viewModel.onInsightActionClicked(insight)
        }

        binding.insightsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = insightsAdapter
        }
    }

    private fun setupClickListeners() {
        binding.btnViewAllSuggestions.setOnClickListener {
            viewModel.onViewAllSuggestionsClicked()

            // TODO: Naviga verso StatisticsFragment con tab Previsioni selezionata
            // findNavController().navigate(R.id.action_home_to_statistics)

            Timber.tag(TAG).d("Navigating to all suggestions")
        }
    }

    private fun observeViewModel() {
        // Osserva il viaggio corrente (logica esistente)
        viewModel.tripToShow.observe(viewLifecycleOwner) { trip ->
            if (trip != null) {
                setupTripCard(trip)
            } else {
                // Nessun viaggio
                binding.cardTrip.visibility = View.GONE
                binding.tvNoTrip.visibility = View.VISIBLE
            }
        }

        // Osserva i suggerimenti
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.suggestions.collect { suggestions ->
                suggestionsAdapter.submitList(suggestions)

                // Aggiorna il contatore
                binding.suggestionsCount.text = suggestions.size.toString()
                binding.suggestionsCount.visibility = if (suggestions.isNotEmpty()) View.VISIBLE else View.GONE
            }
        }

        // Osserva gli insights
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.insights.collect { insights ->
                insightsAdapter.submitList(insights)
            }
        }

        // Osserva lo stato UI
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { uiState ->
                // Mostra/nascondi sezioni
                binding.suggestionsSection.visibility =
                    if (uiState.showSuggestions) View.VISIBLE else View.GONE

                binding.insightsSection.visibility =
                    if (uiState.showInsights) View.VISIBLE else View.GONE

                binding.btnViewAllSuggestions.visibility =
                    if (uiState.showSuggestions) View.VISIBLE else View.GONE
            }
        }
    }

    private fun setupTripCard(trip: com.example.travel_companion.data.local.entity.TripEntity) {
        binding.cardTrip.visibility = View.VISIBLE
        binding.tvNoTrip.visibility = View.GONE

        // Destinazione
        binding.tvDestination.text = trip.destination

        // Date formato "data ora – data ora"
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val start = dateFormat.format(Date(trip.startDate))
        val end = dateFormat.format(Date(trip.endDate))
        binding.tvDates.text = "$start – $end"

        // Stato viaggio basato sul campo status nel database
        when (trip.status) {
            TripStatus.PLANNED -> {
                binding.tvTripStatus.text = "PROGRAMMATO"
                binding.tvTripStatus.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.purple_700)
                )
            }
            TripStatus.STARTED -> {
                binding.tvTripStatus.text = "IN CORSO"
                binding.tvTripStatus.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.green)
                )
            }
            TripStatus.PAUSED -> {
                binding.tvTripStatus.text = "IN PAUSA"
                binding.tvTripStatus.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.red)
                )
            }
            TripStatus.FINISHED -> {
                binding.tvTripStatus.text = "COMPLETATO"
                binding.tvTripStatus.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.gray_dark)
                )
            }
        }

        // Immagine
        if (trip.imageData != null) {
            val bitmap = BitmapFactory.decodeByteArray(trip.imageData, 0, trip.imageData.size)
            binding.ivTripImage.setImageBitmap(bitmap)
            binding.viewImagePlaceholder.visibility = View.GONE
        } else {
            binding.ivTripImage.setImageDrawable(null)
            binding.viewImagePlaceholder.visibility = View.VISIBLE
        }

        binding.cardTrip.setOnClickListener {
            // Navigazione ai dettagli
            try {
                findNavController().navigate(
                    HomeFragmentDirections.actionTripsFragmentToTripDetailFragment(trip.id)
                )
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Errore nella navigazione ai dettagli del viaggio")
                Snackbar.make(binding.root, "Errore nell'apertura dei dettagli", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}