package com.example.travel_companion.presentation.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
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
                // Mostra la card del viaggio
                binding.cardTrip.visibility = View.VISIBLE
                binding.tvNoTrip.visibility = View.GONE

                binding.tvDestination.text = trip.destination

                val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                val start = dateFormat.format(Date(trip.startDate))
                val end = dateFormat.format(Date(trip.endDate))
                binding.tvDates.text = "Dal $start al $end"

                // Stato IN CORSO
                binding.tvTripStatus.text = "IN CORSO"
                binding.tvTripStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.green))
                binding.imgStatus.setColorFilter(ContextCompat.getColor(requireContext(), R.color.green))

            } else {
                // Nessun viaggio oggi
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
