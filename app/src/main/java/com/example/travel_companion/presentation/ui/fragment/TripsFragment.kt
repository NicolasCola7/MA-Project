package com.example.travel_companion.presentation.ui.fragment

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
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TripsFragment : Fragment() {
    private var _binding: FragmentTripsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TripsViewModel by viewModels()
    private lateinit var adapter: TripListAdapter

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

        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
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
        viewModel.trips.observe(viewLifecycleOwner) { tripList ->
            adapter.submitList(tripList) {
                // Dopo aver aggiornato la lista, verifica se ci sono selezioni da mantenere
                adapter.updateSelectionAfterListChange()
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
        binding.deleteSelectedTrips.isVisible = selectedCount > 0

        if (selectedCount > 0) {
            binding.deleteSelectedTrips.text = "Elimina ( $selectedCount)"
        }
    }

    private fun handleMultipleDelete() {
        val selectedTrips = adapter.getSelectedTrips()
        if (selectedTrips.isEmpty()) return

        Utils.SelectionHelper.showMultipleDeleteConfirmation(
            context = requireContext(),
            count = selectedTrips.size,
            itemType = "viaggi",
            onConfirmed = {
                deleteSelectedTrips(selectedTrips)
            }
        )
    }

    private fun deleteSelectedTrips(trips: List<TripEntity>) {
        // Estrai solo gli ID per l'operazione batch
        val tripIds = trips.map { it.id }

        // Operazione batch più efficiente
        viewModel.deleteTrips(tripIds)

        // Pulisci la selezione e aggiorna l'UI
        adapter.clearSelection()
        updateDeleteButton(0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}