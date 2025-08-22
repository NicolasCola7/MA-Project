package com.example.travel_companion.presentation.ui.fragment

import android.os.Bundle
import android.view.*
import android.widget.SearchView
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
import com.google.android.material.datepicker.MaterialDatePicker
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class TripsFragment : Fragment() {
    private var _binding: FragmentTripsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TripsViewModel by viewModels()
    private lateinit var adapter: TripListAdapter

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    // Variabili temporanee per l'overlay
    private var tempStartDate: Long? = null
    private var tempEndDate: Long? = null

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

        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
    }

    private fun setupFiltersOverlay() {
        // Bottone per aprire l'overlay
        binding.filterButton.setOnClickListener {
            showFiltersOverlay()
        }

        // Chiudi overlay cliccando fuori
        binding.filtersOverlay.setOnClickListener {
            hideFiltersOverlay()
        }

        // Evita che il click sulla card chiuda l'overlay
        binding.filtersOverlay.findViewById<androidx.cardview.widget.CardView>(R.id.cardTrip)?.setOnClickListener {
            // Non fare nulla, evita la propagazione del click
        }

        // Setup filtro destinazione nell'overlay
        binding.searchDestination.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                // Aggiorna in tempo reale mentre si digita
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
            clearFiltersInOverlay()
        }

        binding.applyFilters.setOnClickListener {
            applyFiltersAndClose()
        }
    }

    private fun showFiltersOverlay() {
        // Reset valori temporanei ai valori attuali
        resetOverlayToCurrentFilters()
        binding.filtersOverlay.isVisible = true
    }

    private fun hideFiltersOverlay() {
        binding.filtersOverlay.isVisible = false
    }

    private fun resetOverlayToCurrentFilters() {
        // Reset dei valori temporanei
        tempStartDate = null
        tempEndDate = null

        // Reset UI nell'overlay
        binding.searchDestination.setText("")
        binding.filterStartDate.text = "Data inizio"
        binding.filterEndDate.text = "Data fine"
    }

    private fun clearFiltersInOverlay() {
        tempStartDate = null
        tempEndDate = null
        binding.searchDestination.setText("")
        binding.filterStartDate.text = "Data inizio"
        binding.filterEndDate.text = "Data fine"
    }

    private fun applyFiltersAndClose() {
        val destination = binding.searchDestination.text.toString()
        viewModel.applyFilters(tempStartDate, tempEndDate, destination)
        hideFiltersOverlay()
    }

    private fun showDatePicker(isStartDate: Boolean) {
        val title = if (isStartDate) "Seleziona data inizio" else "Seleziona data fine"
        val tag = if (isStartDate) "START_DATE_PICKER" else "END_DATE_PICKER"

        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(title)
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            .build()

        datePicker.addOnPositiveButtonClickListener { dateInMillis ->
            if (isStartDate) {
                tempStartDate = dateInMillis
                binding.filterStartDate.text = "Da: ${dateFormat.format(Date(dateInMillis))}"
            } else {
                tempEndDate = dateInMillis
                binding.filterEndDate.text = "A: ${dateFormat.format(Date(dateInMillis))}"
            }
        }

        datePicker.show(childFragmentManager, tag)
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
        viewModel.filteredTrips.observe(viewLifecycleOwner) { tripList ->
            adapter.submitList(tripList) {
                adapter.updateSelectionAfterListChange()
            }

            // Mostra messaggio se non ci sono risultati
            binding.emptyStateText.isVisible = tripList.isEmpty()
            binding.emptyStateText.text = when {
                tripList.isEmpty() -> "Non hai ancora pianificato nessun viaggio"
                else -> ""
            }
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
        viewModel.deleteTrips(tripIds)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}