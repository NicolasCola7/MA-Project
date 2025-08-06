package com.example.travel_companion.presentation.ui.fragment

import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AlertDialog
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
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TripsFragment : Fragment() {
    private var _binding: FragmentTripsBinding? = null
    private val binding get() = _binding!!

    val viewModel: TripsViewModel by viewModels()
    lateinit var adapter: TripListAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(inflater, R.layout.fragment_trips, container, false)

        binding.addTrip.setOnClickListener {
            val action = TripsFragmentDirections.actionTripsFragmentToNewTripFragment()
            findNavController().navigate(action)
        }

        adapter = TripListAdapter(
            onTripClick = { trip ->
                val action = TripsFragmentDirections.actionTripsFragmentToTripDetailFragment(trip.id)
                findNavController().navigate(action)
            },
            onSelectionChanged = { count ->
                updateDeleteButton(count)
            }
        )

        binding.recyclerView.adapter = adapter
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        binding.deleteSelectedTrips.setOnClickListener {
            val selectedIds = adapter.getSelectedTrips()
            if (selectedIds.isNotEmpty()) {
                showDeleteConfirmationMultiple(selectedIds)
            }
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.trips.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
        }
    }

    private fun updateDeleteButton(selectedCount: Int) {
        binding.deleteSelectedTrips.isVisible = selectedCount > 0
        binding.deleteSelectedTrips.text = "Elimina ($selectedCount)"
    }

    private fun showDeleteConfirmationMultiple(trips: List<TripEntity>) {
        AlertDialog.Builder(requireContext())
            .setTitle("Elimina viaggi")
            .setMessage("Sei sicuro di voler eliminare ${trips.size} viaggi selezionati?")
            .setPositiveButton("Elimina") { _, _ ->
                trips.forEach { trip -> viewModel.deleteTripById(trip) }
                adapter.clearSelection()
            }
            .setNegativeButton("Annulla", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
