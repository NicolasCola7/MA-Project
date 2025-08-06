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
import androidx.navigation.fragment.findNavController
import com.example.travel_companion.R
import com.example.travel_companion.databinding.FragmentHomeBinding
import com.example.travel_companion.domain.model.TripStatus
import com.example.travel_companion.presentation.viewmodel.HomeViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HomeViewModel by viewModels()

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

        viewModel.tripToShow.observe(viewLifecycleOwner) { trip ->
            if (trip != null) {
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
                    findNavController().navigate(
                        HomeFragmentDirections.actionTripsFragmentToTripDetailFragment(trip.id,"home")
                    )
                }

            } else {
                // Nessun viaggio
                binding.cardTrip.visibility = View.GONE
                binding.tvNoTrip.visibility = View.VISIBLE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}