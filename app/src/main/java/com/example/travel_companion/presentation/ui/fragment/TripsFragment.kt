package com.example.travel_companion.presentation.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.travel_companion.R
import com.example.travel_companion.databinding.FragmentTripsBinding
import com.example.travel_companion.presentation.adapter.TripListAdapter
import com.example.travel_companion.presentation.viewmodel.TripsViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TripsFragment: Fragment() {
    private  var _binding: FragmentTripsBinding? = null
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

        adapter = TripListAdapter(emptyList()) { selectedTrip ->
            //creazione dell'azione "navigazione" con passaggio di parametro
            val action = TripsFragmentDirections.actionTripsFragmentToTripDetailFragment(selectedTrip.id)
            //navigazione effettiva
            findNavController().navigate(action)
        }
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        binding.recyclerView.adapter = adapter

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        //osserva i dati dal viewmodel
        viewModel.trips.observe(viewLifecycleOwner) { list ->
            adapter.update(list)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}