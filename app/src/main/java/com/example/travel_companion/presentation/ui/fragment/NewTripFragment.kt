package com.example.travel_companion.presentation.ui.fragment

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
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

        binding.editStartDate.setOnClickListener { showDateTimePicker(binding.editStartDate) }
        binding.editEndDate.setOnClickListener { showDateTimePicker(binding.editEndDate) }

        binding.spinnerTripType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selected = parent.getItemAtPosition(position).toString()
                if (selected == "Viaggio di più giorni") {
                    binding.editEndDate.visibility = View.VISIBLE
                } else {
                    binding.editEndDate.visibility = View.GONE
                    binding.editEndDate.setText("")
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        binding.btnCreateTrip.setOnClickListener {
            val destination = binding.editDestination.text.toString()
            val startStr = binding.editStartDate.text.toString()
            val endStr = binding.editEndDate.text.toString()
            val type = binding.spinnerTripType.selectedItem.toString()

            if (destination.isBlank() || startStr.isBlank() || (type == "Viaggio di più giorni" && endStr.isBlank())) {
                Toast.makeText(requireContext(), "Compila i campi obbligatori", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val startDate = Utils.dateTimeFormat.parse(startStr)
            val endDate = if (type == "Viaggio di più giorni") Utils.dateFormat.parse(endStr) else null

            if (startDate == null || (type == "Viaggio di più giorni" && endDate == null)) {
                Toast.makeText(requireContext(), "Formato data non valido", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val now = System.currentTimeMillis()
            if (startDate.time <= now) {
                Toast.makeText(requireContext(), "La data di inizio deve essere futura", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (type == "Viaggio di più giorni" && endDate!!.time <= startDate.time) {
                Toast.makeText(requireContext(), "La data di fine deve essere dopo la data di inizio", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val finalEndDate = if (type == "Viaggio di più giorni") {
                endDate!!.time
            } else {
                // fine del giorno
                val calendar = Calendar.getInstance().apply {
                    time = startDate
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                }
                calendar.timeInMillis
            }

            viewModel.insertTrip(destination, startDate.time, finalEndDate, type)
            findNavController().navigateUp()
        }

    }

    private fun showDateTimePicker(target: EditText) {
        val calendar = Calendar.getInstance()


        DatePickerDialog(requireContext(), { _, year, month, day ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, day)


            TimePickerDialog(requireContext(), { _, hourOfDay, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                calendar.set(Calendar.MINUTE, minute)
                calendar.set(Calendar.SECOND, 0)

                
                target.setText(Utils.dateTimeFormat.format(calendar.time))
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()

        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}