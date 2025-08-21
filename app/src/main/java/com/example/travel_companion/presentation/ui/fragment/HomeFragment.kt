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
import com.example.travel_companion.databinding.FragmentHomeBinding
import com.example.travel_companion.domain.model.TripStatus
import com.example.travel_companion.presentation.adapter.SuggestionsAdapter
import com.example.travel_companion.presentation.viewmodel.HomeViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

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

    private fun setupRecyclerViews() {
        binding.suggestionsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = suggestionsAdapter
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
            }
        }

        // Osserva se mostrare la sezione suggerimenti
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.showSuggestions.collect { show ->
                binding.suggestionsSection.visibility = if (show) View.VISIBLE else View.GONE
            }
        }

        // Osserva gli errori
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.error.collect { error ->
                if (error != null) {
                    Snackbar.make(binding.root, error, Snackbar.LENGTH_LONG).show()
                    viewModel.dismissError()
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setupTripCard(trip: com.example.travel_companion.data.local.entity.TripEntity) {
        binding.cardTrip.visibility = View.VISIBLE
        binding.tvNoTrip.visibility = View.GONE

        // Destinazione
        binding.tvDestination.text = trip.destination

        // Date formato "data ora – data ora"
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val start = dateFormat.format(Date(trip.startDate))
        val end = dateFormat.format(Date(trip.endDate))
        binding.tvDates.text = "$start $end"

        // Gestione immagine e overlay
        if (trip.imageData != null) {
            // Ha immagine: mostra immagine con overlay e testo bianco
            val bitmap = BitmapFactory.decodeByteArray(trip.imageData, 0, trip.imageData.size)
            binding.ivTripImage.setImageBitmap(bitmap)
            binding.viewImagePlaceholder.visibility = View.GONE

            // Mostra overlay per leggibilità testo
            binding.root.findViewById<View>(R.id.overlay_view)?.visibility = View.VISIBLE

            // Testo bianco con ombra
            binding.tvDestination.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
            binding.tvDates.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))

        } else {
            // Nessuna immagine: placeholder senza overlay e testo normale
            binding.ivTripImage.setImageDrawable(null)
            binding.viewImagePlaceholder.visibility = View.VISIBLE

            // Nascondi overlay
            binding.root.findViewById<View>(R.id.overlay_view)?.visibility = View.GONE

            // Testo con colori normali
            binding.tvDestination.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
            binding.tvDates.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
        }

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