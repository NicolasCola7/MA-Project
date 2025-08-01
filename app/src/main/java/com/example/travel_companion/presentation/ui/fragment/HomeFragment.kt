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
import com.bumptech.glide.Glide
import com.example.travel_companion.R
import com.example.travel_companion.databinding.FragmentHomeBinding
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

        viewModel.todayTrip.observe(viewLifecycleOwner) { trip ->
            if (trip != null) {
                binding.cardTrip.visibility = View.VISIBLE
                binding.tvNoTrip.visibility = View.GONE

                // Destinazione
                binding.tvDestination.text = trip.destination

                // Date
                val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                binding.tvDates.text = "${dateFormat.format(Date(trip.startDate))} – ${dateFormat.format(Date(trip.endDate))}"

                // Stato
                binding.tvTripStatus.text = "IN CORSO"
                binding.tvTripStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.green))

                // Immagine o placeholder
                if (trip.imageData != null) {
                    binding.viewImagePlaceholder.visibility = View.GONE
                    val bmp = BitmapFactory.decodeByteArray(trip.imageData, 0, trip.imageData.size)
                    binding.ivTripImage.setImageBitmap(bmp)
                } else {
                    binding.viewImagePlaceholder.visibility = View.VISIBLE
                    binding.ivTripImage.setImageDrawable(null)
                }

                // Click → dettagli viaggio
                binding.cardTrip.setOnClickListener {
                    // Navigazione ai dettagli
                    findNavController().navigate(
                        HomeFragmentDirections.actionTripsFragmentToTripDetailFragment(trip.id)
                    )
                }

            } else {
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
