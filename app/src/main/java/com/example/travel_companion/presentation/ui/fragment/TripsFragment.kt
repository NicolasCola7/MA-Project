package com.example.travel_companion.presentation.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
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
class TripsFragment: Fragment() {
    private var _binding: FragmentTripsBinding? = null
    private val binding get() = _binding!!

    val viewModel: TripsViewModel by viewModels()
    lateinit var adapter: TripListAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_trips, container, false
        )

        binding.addTrip.setOnClickListener {
            val action = TripsFragmentDirections.actionTripsFragmentToNewTripFragment()
            findNavController().navigate(action)
        }

        adapter = TripListAdapter(
            onTripClick = { trip ->
                val action = TripsFragmentDirections.actionTripsFragmentToTripDetailFragment(trip.id)
                findNavController().navigate(action)
            },
            onTripLongClick = { trip, view ->
                showDeleteOverlay(trip, view)
            }
        )

        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        binding.recyclerView.adapter = adapter

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.trips.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
        }
    }

    private fun showDeleteOverlay(trip: TripEntity, anchorView: View) {
        val popupMenu = PopupMenu(requireContext(), anchorView)
        popupMenu.menuInflater.inflate(R.menu.trip_context_menu, popupMenu.menu)

        // Forza la visualizzazione delle icone
        try {
            val fieldMPopup = PopupMenu::class.java.getDeclaredField("mPopup")
            fieldMPopup.isAccessible = true
            val mPopup = fieldMPopup.get(popupMenu)
            mPopup.javaClass
                .getDeclaredMethod("setForceShowIcon", Boolean::class.java)
                .invoke(mPopup, true)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_delete -> {
                    showDeleteConfirmation(trip)
                    true
                }
                else -> false
            }
        }

        popupMenu.show()
    }

    private fun showDeleteConfirmation(trip: TripEntity) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Elimina viaggio")
            .setMessage("Sei sicuro di voler eliminare il viaggio per \"${trip.destination}\"?")
            .setPositiveButton("Elimina") { _, _ ->
                viewModel.deleteTrip(trip)
            }
            .setNegativeButton("Annulla", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}