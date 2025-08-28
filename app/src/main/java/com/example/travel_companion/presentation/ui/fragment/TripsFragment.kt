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
import com.example.travel_companion.presentation.adapter.TripListAdapter
import com.example.travel_companion.presentation.viewmodel.TripsViewModel
import com.example.travel_companion.presentation.viewmodel.FiltersViewModel
import com.example.travel_companion.util.helpers.EmptyStateHelper
import com.example.travel_companion.util.helpers.SelectionHelper
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

    /**
     * Inflates the fragment layout and initializes view setup.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(inflater, R.layout.fragment_trips, container, false)
        setupViews()
        return binding.root
    }

    /**
     * Observes data and initializes UI behavior after view creation.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeData()
    }

    /**
     * Sets up view interactions, adapters, and RecyclerView.
     */
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

    /**
     * Configures the filters overlay UI and interactions.
     */
    private fun setupFiltersOverlay() {
        binding.filterButton.setOnClickListener { showFiltersOverlay() }
        binding.closeFiltersButton.setOnClickListener { hideFiltersOverlay() }
        binding.filtersCard.setOnClickListener { }

        binding.searchDestination.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                filtersViewModel.setTempDestination(s?.toString() ?: "")
            }
        })

        binding.filterStartDate.setOnClickListener { showDatePicker(isStartDate = true) }
        binding.filterEndDate.setOnClickListener { showDatePicker(isStartDate = false) }

        binding.clearFilters.setOnClickListener {
            filtersViewModel.resetTempFilters()
            updateFiltersUI()
        }

        binding.applyFilters.setOnClickListener {
            filtersViewModel.applyFilters()
            hideFiltersOverlay()
        }

        binding.clearFilters.setOnLongClickListener {
            filtersViewModel.clearAllFilters()
            updateFiltersUI()
            true
        }
    }

    /**
     * Shows the filters overlay and updates the UI accordingly.
     */
    private fun showFiltersOverlay() {
        filtersViewModel.loadAppliedFiltersToTemp()
        updateFiltersUI()
        binding.filtersOverlay.isVisible = true
        EmptyStateHelper.hideEmptyState(binding.emptyStateLayout.root)
    }

    /**
     * Hides the filters overlay and updates empty state visibility.
     */
    private fun hideFiltersOverlay() {
        hideKeyboard()
        binding.filtersOverlay.isVisible = false

        val allTrips = tripsViewModel.trips.value ?: emptyList()
        val filteredTrips = filtersViewModel.filterTrips(allTrips)
        val shouldShowEmptyState = filteredTrips.isEmpty()

        if (shouldShowEmptyState) {
            EmptyStateHelper.showTripsEmptyState(
                binding.emptyStateLayout.root,
                filtersViewModel.hasActiveFilters()
            )
        } else {
            EmptyStateHelper.hideEmptyState(binding.emptyStateLayout.root)
        }
    }

    /**
     * Updates the filters UI based on temporary filter values.
     */
    private fun updateFiltersUI() {
        binding.searchDestination.setText(filtersViewModel.tempDestination.value ?: "")
        binding.filterStartDate.text = filtersViewModel.tempStartDate.value?.let {
            "Da: ${dateFormat.format(Date(it))}"
        } ?: "Data inizio"
        binding.filterEndDate.text = filtersViewModel.tempEndDate.value?.let {
            "A: ${dateFormat.format(Date(it))}"
        } ?: "Data fine"
    }

    /**
     * Displays a date picker for selecting start or end date.
     * @param isStartDate True for start date picker, false for end date picker.
     */
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

    /**
     * Hides the soft keyboard if visible.
     */
    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(binding.searchDestination.windowToken, 0)
    }

    /**
     * Initializes the RecyclerView adapter with click and selection listeners.
     */
    private fun setupAdapter() {
        adapter = TripListAdapter(
            onTripClick = { trip ->
                if (!adapter.isSelectionMode) {
                    navigateToTripDetail(trip.id)
                }
            },
            onSelectionChanged = { count ->
                updateDeleteButton(count)
            }
        )
    }

    /**
     * Binds the adapter to the RecyclerView.
     */
    private fun setupRecyclerView() {
        binding.recyclerView.adapter = adapter
    }

    /**
     * Sets up the delete button behavior for selected trips.
     */
    private fun setupDeleteButton() {
        binding.deleteSelectedTrips.setOnClickListener {
            handleMultipleDelete()
        }
    }

    /**
     * Observes trips and filter events to update the displayed list.
     */
    private fun observeData() {
        tripsViewModel.trips.observe(viewLifecycleOwner) { allTrips ->
            val filteredTrips = filtersViewModel.filterTrips(allTrips)
            updateTripsList(filteredTrips)
        }

        filtersViewModel.filtersEvent.observe(viewLifecycleOwner) { event ->
            when (event) {
                is FiltersViewModel.FiltersEvent.FiltersApplied,
                FiltersViewModel.FiltersEvent.FiltersCleared -> {
                    val allTrips = tripsViewModel.trips.value ?: emptyList()
                    val filteredTrips = filtersViewModel.filterTrips(allTrips)
                    updateTripsList(filteredTrips)
                }
            }
        }
    }

    /**
     * Updates the RecyclerView with a filtered list of trips.
     * @param filteredTrips List of trips to display.
     */
    private fun updateTripsList(filteredTrips: List<TripEntity>) {
        adapter.submitList(filteredTrips) { adapter.updateSelectionAfterListChange() }

        val shouldShowEmptyState = filteredTrips.isEmpty() && !binding.filtersOverlay.isVisible

        if (shouldShowEmptyState) {
            EmptyStateHelper.showTripsEmptyState(binding.emptyStateLayout.root, filtersViewModel.hasActiveFilters())
        } else {
            EmptyStateHelper.hideEmptyState(binding.emptyStateLayout.root)
        }
    }

    /**
     * Navigates to the fragment for creating a new trip.
     */
    private fun navigateToNewTrip() {
        val action = TripsFragmentDirections.actionTripsFragmentToNewTripFragment()
        findNavController().navigate(action)
    }

    /**
     * Navigates to the detail fragment for a specific trip.
     * @param tripId ID of the trip to view.
     */
    private fun navigateToTripDetail(tripId: Long) {
        val action = TripsFragmentDirections.actionTripsFragmentToTripDetailFragment(tripId)
        findNavController().navigate(action)
    }

    /**
     * Updates the delete button text based on selected count.
     * @param selectedCount Number of selected items.
     */
    private fun updateDeleteButton(selectedCount: Int) {
        SelectionHelper.updateDeleteButton(
            button = binding.deleteSelectedTrips,
            selectedCount = selectedCount,
            baseText = "Elimina"
        )
    }

    /**
     * Handles deletion of multiple selected trips.
     */
    private fun handleMultipleDelete() {
        val selectedTrips = adapter.getSelectedTrips()
        SelectionHelper.handleMultipleDelete(
            context = requireContext(),
            selectedItems = selectedTrips,
            itemType = "viaggi",
            onDelete = { trips -> deleteSelectedTrips(trips) },
            onClearSelection = { adapter.clearSelection() },
            onUpdateButton = { count -> updateDeleteButton(count) }
        )
    }

    /**
     * Deletes a list of trips via the ViewModel.
     * @param trips List of trips to delete.
     */
    private fun deleteSelectedTrips(trips: List<TripEntity>) {
        val tripIds = trips.map { it.id }
        tripsViewModel.deleteTrips(tripIds)
    }

    /**
     * Cleans up the binding when the view is destroyed.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
