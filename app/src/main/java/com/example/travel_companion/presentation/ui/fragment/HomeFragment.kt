package com.example.travel_companion.presentation.ui.fragment

import android.annotation.SuppressLint
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
import com.example.travel_companion.data.local.entity.TripEntity
import com.example.travel_companion.databinding.FragmentHomeBinding
import com.example.travel_companion.domain.model.TripStatus
import com.example.travel_companion.presentation.adapter.SuggestionsAdapter
import com.example.travel_companion.presentation.viewmodel.HomeViewModel
import com.example.travel_companion.util.helpers.EmptyStateHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Home screen fragment showing the current trip card and suggested trips.
 * Handles UI updates, trip selection, and navigation to other screens.
 */
@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()
    private lateinit var suggestionsAdapter: SuggestionsAdapter

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

        suggestionsAdapter = SuggestionsAdapter()
        setupRecyclerViews()
        observeViewModel()
    }

    /** Sets up RecyclerView for the suggestions section. */
    private fun setupRecyclerViews() {
        binding.suggestionsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = suggestionsAdapter
        }
    }

    /** Observes LiveData and StateFlow from the ViewModel to update the UI. */
    private fun observeViewModel() {
        // Observe the current trip
        viewModel.tripToShow.observe(viewLifecycleOwner) { trip ->
            if (trip != null) {
                setupTripCard(trip)
                // Show the trip card and hide empty state
                binding.cardTrip.visibility = View.VISIBLE
                EmptyStateHelper.hideEmptyState(binding.emptyStateLayout.root)
            } else {
                // No trip: hide card and show empty state
                binding.cardTrip.visibility = View.GONE
                EmptyStateHelper.showHomeEmptyState(binding.emptyStateLayout.root) {
                    navigateToNewTrip()
                }
            }
        }

        // Observe suggested trips
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.suggestions.collect { suggestions ->
                suggestionsAdapter.submitList(suggestions)
            }
        }

        // Observe whether the suggestions section should be visible
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.showSuggestions.collect { show ->
                binding.suggestionsSection.visibility = if (show) View.VISIBLE else View.GONE
            }
        }
    }

    /** Navigates to the new trip creation screen. */
    private fun navigateToNewTrip() {
        findNavController().navigate(HomeFragmentDirections.actionHomeFragmentToNewTripFragment())
    }

    /**
     * Configures the trip card with the trip information.
     *
     * @param trip The trip entity to display in the card.
     */
    @SuppressLint("SetTextI18n")
    private fun setupTripCard(trip: TripEntity) {
        // Destination
        binding.tvDestination.text = trip.destination

        // Date format "start date – end date"
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val start = dateFormat.format(Date(trip.startDate))
        val end = dateFormat.format(Date(trip.endDate))
        binding.tvDates.text = "$start $end"

        // Handle trip image and overlay
        if (trip.imageData != null) {
            val bitmap = BitmapFactory.decodeByteArray(trip.imageData, 0, trip.imageData.size)
            binding.ivTripImage.setImageBitmap(bitmap)
            binding.viewImagePlaceholder.visibility = View.GONE
            binding.root.findViewById<View>(R.id.overlay_view)?.visibility = View.VISIBLE
            binding.tvDestination.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
            binding.tvDates.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
        } else {
            binding.ivTripImage.setImageDrawable(null)
            binding.viewImagePlaceholder.visibility = View.VISIBLE
            binding.root.findViewById<View>(R.id.overlay_view)?.visibility = View.GONE
            binding.tvDestination.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
            binding.tvDates.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
        }

        // Set trip status text and color
        when (trip.status) {
            TripStatus.PLANNED -> {
                binding.tvTripStatus.text = "PROGRAMMATO"
                binding.tvTripStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.purple_700))
            }
            TripStatus.STARTED -> {
                binding.tvTripStatus.text = "IN CORSO"
                binding.tvTripStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.green))
            }
            TripStatus.FINISHED -> {
                binding.tvTripStatus.text = "COMPLETATO"
                binding.tvTripStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray_dark))
            }
        }

        // Navigate to trip detail on card click
        binding.cardTrip.setOnClickListener {
            findNavController().navigate(
                HomeFragmentDirections.actionTripsFragmentToTripDetailFragment(trip.id)
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
