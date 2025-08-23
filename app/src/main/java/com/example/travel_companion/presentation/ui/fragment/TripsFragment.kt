package com.example.travel_companion.presentation.ui.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.*
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.travel_companion.R
import com.example.travel_companion.data.local.entity.TripEntity
import com.example.travel_companion.databinding.FragmentTripsBinding
import com.example.travel_companion.util.Utils
import com.example.travel_companion.presentation.adapter.TripListAdapter
import com.example.travel_companion.presentation.viewmodel.TripsViewModel
import com.example.travel_companion.presentation.viewmodel.FiltersViewModel
import com.google.android.material.datepicker.MaterialDatePicker
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class TripsFragment : Fragment() {
    private var _binding: FragmentTripsBinding? = null
    private val binding get() = _binding!!

    private val tripsViewModel: TripsViewModel by viewModels()
    private val filtersViewModel: FiltersViewModel by viewModels()
    private lateinit var adapter: TripListAdapter

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(inflater, R.layout.fragment_trips, container, false)
        setupViews()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeData()
    }

    private fun setupViews() {
        binding.addTrip.setOnClickListener {
            navigateToNewTrip()
        }

        setupAdapter()
        setupRecyclerView()
        setupDeleteButton()
        setupFiltersOverlay()

        binding.viewModel = tripsViewModel
        binding.lifecycleOwner = viewLifecycleOwner
    }

    private fun setupFiltersOverlay() {
        // Bottone per aprire l'overlay
        binding.filterButton.setOnClickListener {
            showFiltersOverlay()
        }

        // Freccia per chiudere l'overlay
        binding.closeFiltersButton.setOnClickListener {
            hideFiltersOverlay()
        }

        // Evita che il click sulla card chiuda l'overlay
        binding.filtersCard.setOnClickListener {
            // Non fare nulla, evita la propagazione del click
        }

        // Setup filtro destinazione nell'overlay - collegato al FiltersViewModel
        binding.searchDestination.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                filtersViewModel.setTempDestination(s?.toString() ?: "")
            }
        })

        // Setup filtri data nell'overlay
        binding.filterStartDate.setOnClickListener {
            showDatePicker(isStartDate = true)
        }

        binding.filterEndDate.setOnClickListener {
            showDatePicker(isStartDate = false)
        }

        // Setup pulsanti azione
        binding.clearFilters.setOnClickListener {
            filtersViewModel.resetTempFilters()
            updateFiltersUI()
        }

        binding.applyFilters.setOnClickListener {
            filtersViewModel.applyFilters()
            hideKeyboard()
            hideFiltersOverlay()
        }

        // Long click per pulire tutti i filtri
        binding.clearFilters.setOnLongClickListener {
            filtersViewModel.clearAllFilters()
            updateFiltersUI()
            true
        }
    }

    private fun showFiltersOverlay() {
        // Carica i filtri applicati nei temporanei
        filtersViewModel.loadAppliedFiltersToTemp()
        updateFiltersUI()
        binding.filtersOverlay.isVisible = true

        // Nascondi lo stato vuoto quando l'overlay è aperto
        binding.emptyStateText.isVisible = false
    }

    private fun hideFiltersOverlay() {
        binding.filtersOverlay.isVisible = false

        // Rimostra lo stato vuoto se necessario dopo la chiusura dell'overlay
        // Applica i filtri tramite FiltersViewModel per ottenere i risultati corretti
        val allTrips = tripsViewModel.trips.value ?: emptyList()
        val filteredTrips = filtersViewModel.filterTrips(allTrips)
        val shouldShowEmptyState = filteredTrips.isEmpty()

        binding.emptyStateText.isVisible = shouldShowEmptyState
        binding.emptyStateText.text = when {
            filteredTrips.isEmpty() && filtersViewModel.hasActiveFilters() -> "Nessun viaggio trovato con i filtri attuali"
            filteredTrips.isEmpty() -> "Non hai ancora pianificato nessun viaggio"
            else -> ""
        }
    }

    private fun updateFiltersUI() {
        // Aggiorna UI basandosi sui filtri temporanei
        binding.searchDestination.setText(filtersViewModel.tempDestination.value ?: "")

        binding.filterStartDate.text = if (filtersViewModel.tempStartDate.value != null) {
            "Da: ${dateFormat.format(Date(filtersViewModel.tempStartDate.value!!))}"
        } else {
            "Data inizio"
        }

        binding.filterEndDate.text = if (filtersViewModel.tempEndDate.value != null) {
            "A: ${dateFormat.format(Date(filtersViewModel.tempEndDate.value!!))}"
        } else {
            "Data fine"
        }
    }

    @SuppressLint("SetTextI18n")
    private fun showDatePicker(isStartDate: Boolean) {
        val title = if (isStartDate) "Seleziona data inizio" else "Seleziona data fine"
        val tag = if (isStartDate) "START_DATE_PICKER" else "END_DATE_PICKER"

        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(title)
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            .build()

        datePicker.addOnPositiveButtonClickListener { dateInMillis ->
            if (isStartDate) {
                filtersViewModel.setTempStartDate(dateInMillis)
                binding.filterStartDate.text = "Da: ${dateFormat.format(Date(dateInMillis))}"
            } else {
                filtersViewModel.setTempEndDate(dateInMillis)
                binding.filterEndDate.text = "A: ${dateFormat.format(Date(dateInMillis))}"
            }
        }

        datePicker.show(childFragmentManager, tag)
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(binding.searchDestination.windowToken, 0)
    }

    private fun setupAdapter() {
        adapter = TripListAdapter(
            onTripClick = { trip ->
                if (!adapter.selectionMode) {
                    navigateToTripDetail(trip.id)
                }
            },
            onSelectionChanged = { count ->
                updateDeleteButton(count)
            }
        )
    }

    private fun setupRecyclerView() {
        binding.recyclerView.adapter = adapter
    }

    private fun setupDeleteButton() {
        binding.deleteSelectedTrips.setOnClickListener {
            handleMultipleDelete()
        }
    }

    private fun observeData() {
        // Osserva i viaggi dal repository
        tripsViewModel.trips.observe(viewLifecycleOwner) { allTrips ->
            // Applica i filtri tramite FiltersViewModel
            val filteredTrips = filtersViewModel.filterTrips(allTrips)
            updateTripsList(filteredTrips)
        }

        // Osserva gli eventi dei filtri per riapplicare i filtri quando cambiano
        filtersViewModel.filtersEvent.observe(viewLifecycleOwner) { event ->
            when (event) {
                is FiltersViewModel.FiltersEvent.FiltersApplied,
                FiltersViewModel.FiltersEvent.FiltersCleared -> {
                    // Riapplica i filtri quando cambiano
                    val allTrips = tripsViewModel.trips.value ?: emptyList()
                    val filteredTrips = filtersViewModel.filterTrips(allTrips)
                    updateTripsList(filteredTrips)
                }
            }
        }
    }

    private fun updateTripsList(filteredTrips: List<TripEntity>) {
        adapter.submitList(filteredTrips) {
            adapter.updateSelectionAfterListChange()
        }

        // Mostra messaggio se non ci sono risultati e l'overlay non è visibile
        val shouldShowEmptyState = filteredTrips.isEmpty() && !binding.filtersOverlay.isVisible
        binding.emptyStateText.isVisible = shouldShowEmptyState

        // Messaggio diverso se ci sono filtri attivi
        binding.emptyStateText.text = when {
            filteredTrips.isEmpty() && filtersViewModel.hasActiveFilters() -> "Nessun viaggio trovato con i filtri attuali"
            filteredTrips.isEmpty() -> "Non hai ancora pianificato nessun viaggio"
            else -> ""
        }
    }

    private fun navigateToNewTrip() {
        val action = TripsFragmentDirections.actionTripsFragmentToNewTripFragment()
        findNavController().navigate(action)
    }

    private fun navigateToTripDetail(tripId: Long) {
        val action = TripsFragmentDirections.actionTripsFragmentToTripDetailFragment(tripId)
        findNavController().navigate(action)
    }

    private fun updateDeleteButton(selectedCount: Int) {
        Utils.SelectionHelper.updateDeleteButton(
            button = binding.deleteSelectedTrips,
            selectedCount = selectedCount,
            baseText = "Elimina"
        )
    }

    private fun handleMultipleDelete() {
        val selectedTrips = adapter.getSelectedTrips()

        Utils.SelectionHelper.handleMultipleDelete(
            context = requireContext(),
            selectedItems = selectedTrips,
            itemType = "viaggi",
            onDelete = { trips -> deleteSelectedTrips(trips) },
            onClearSelection = { adapter.clearSelection() },
            onUpdateButton = { count -> updateDeleteButton(count) }
        )
    }

    private fun deleteSelectedTrips(trips: List<TripEntity>) {
        val tripIds = trips.map { it.id }
        tripsViewModel.deleteTrips(tripIds)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}