package com.example.travel_companion.presentation.ui.fragment

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.travel_companion.R
import com.example.travel_companion.databinding.FragmentNewTripBinding
import com.example.travel_companion.presentation.Utils
import com.example.travel_companion.presentation.viewmodel.TripsViewModel
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import java.util.Calendar

@AndroidEntryPoint
class NewTripFragment: Fragment() {
    private var _binding: FragmentNewTripBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TripsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_new_trip, container, false
        )

        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.editStartDate.setOnClickListener { showDatePicker(binding.editStartDate) }
        binding.editEndDate.setOnClickListener { showDatePicker(binding.editEndDate) }

        binding.btnCreateTrip.setOnClickListener {
            val destination = binding.editDestination.text.toString()
            var startContent = binding.editStartDate.text.toString()
            val end = Utils.dateFormat.parse(binding.editEndDate.text.toString())?.time
            val type = binding.spinnerTripType.selectedItem.toString()

            if (destination.isBlank() || type.isBlank() || startContent.isBlank() ){
                Toast.makeText(requireContext(), "Compila i campi obbligatori", Toast.LENGTH_SHORT).show()
            }

            val parsedStart = Utils.dateFormat.parse(startContent)!!.time
            viewModel.insertTrip(destination, parsedStart, end, type)
            findNavController().navigateUp()
        }
    }

    private fun showDatePicker(target: EditText) {
        val c = Calendar.getInstance()
        DatePickerDialog(requireContext(), { _, y, m, d ->
            c.set(y, m, d)
            target.setText(Utils.dateFormat.format(c.time))
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}