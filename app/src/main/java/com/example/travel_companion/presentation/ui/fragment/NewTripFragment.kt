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
import com.google.android.gms.common.api.Status
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
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
        if (!Places.isInitialized()) {
            Places.initialize(requireContext().applicationContext, "AIzaSyDEVW0HX64ZlwkoVMAZVr7OqgKO4IAuWno")
        }

        val autocompleteFragment = childFragmentManager.findFragmentById(R.id.autocomplete_fragment) as AutocompleteSupportFragment

        autocompleteFragment.setPlaceFields(
            listOf(
                Place.Field.ID,
                Place.Field.NAME,
                Place.Field.ADDRESS,
                Place.Field.LAT_LNG
            )
        )

// Quando l'utente seleziona un luogo dai suggerimenti
        autocompleteFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                Timber.i("Luogo selezionato: ${place.name}, ${place.address}")
                viewModel.selectedDestinationName = place.name ?: ""
            }

            override fun onError(status: Status) {
                Timber.e("Errore Autocomplete: $status")
                Toast.makeText(requireContext(), "Errore: ${status.statusMessage}", Toast.LENGTH_SHORT).show()
            }
        })

        binding.editStartDate.setOnClickListener {
            showDateTimePicker(binding.editStartDate, onlyTime = false)
        }

        binding.editEndDate.setOnClickListener {
            val type = binding.spinnerTripType.selectedItem.toString()
            val onlyTime = type == "Gita Giornaliera" || type == "Viaggio Locale"
            showDateTimePicker(binding.editEndDate, onlyTime)
        }


        binding.spinnerTripType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selected = parent.getItemAtPosition(position).toString()
                when (selected) {
                    "Viaggio di più giorni" -> {
                        binding.editEndDate.visibility = View.VISIBLE
                        binding.editEndDate.hint = "Data e ora fine viaggio"
                    }
                    "Gita Giornaliera", "Viaggio Locale" -> {
                        binding.editEndDate.visibility = View.VISIBLE
                        binding.editEndDate.hint = "Ora fine viaggio"
                    }
                    else -> {
                        binding.editEndDate.visibility = View.GONE
                        binding.editEndDate.setText("")
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        binding.btnCreateTrip.setOnClickListener {
            val destination = viewModel.selectedDestinationName
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

            val endInput = binding.editEndDate.text.toString()
            val calendar = Calendar.getInstance()
            calendar.time = startDate

            val finalEndDate: Long = when (type) {
                "Viaggio di più giorni" -> {
                    if (endInput.isBlank()) {
                        Toast.makeText(requireContext(), "Inserisci la data di fine", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    val parsed = Utils.dateFormat.parse(endInput)
                    if (parsed == null || parsed.time <= startDate.time) {
                        Toast.makeText(requireContext(), "La data di fine deve essere dopo l'inizio", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    parsed.time
                }

                "Gita Giornaliera", "Viaggio Locale" -> {
                    if (endInput.isBlank()) {
                        // fine default: 23:59 dello stesso giorno
                        calendar.set(Calendar.HOUR_OF_DAY, 23)
                        calendar.set(Calendar.MINUTE, 59)
                        calendar.set(Calendar.SECOND, 59)
                        calendar.timeInMillis
                    } else {
                        val timeParts = endInput.split(":")
                        if (timeParts.size != 2) {
                            Toast.makeText(requireContext(), "Inserisci l'ora di fine nel formato HH:mm", Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }
                        val hour = timeParts[0].toIntOrNull()
                        val minute = timeParts[1].toIntOrNull()
                        if (hour == null || minute == null) {
                            Toast.makeText(requireContext(), "Orario non valido", Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }
                        calendar.set(Calendar.HOUR_OF_DAY, hour)
                        calendar.set(Calendar.MINUTE, minute)
                        calendar.set(Calendar.SECOND, 0)

                        val endMillis = calendar.timeInMillis
                        if (endMillis <= startDate.time) {
                            Toast.makeText(requireContext(), "L'ora di fine deve essere successiva a quella di inizio", Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }
                        endMillis
                    }
                }

                else -> {
                    // caso di fallback, mai usato
                    calendar.set(Calendar.HOUR_OF_DAY, 23)
                    calendar.set(Calendar.MINUTE, 59)
                    calendar.set(Calendar.SECOND, 59)
                    calendar.timeInMillis
                }
            }


            viewModel.insertTrip(destination, startDate.time, finalEndDate, type)
            findNavController().navigateUp()
        }

    }

    private fun showDateTimePicker(target: EditText, onlyTime: Boolean = false) {
        val calendar = Calendar.getInstance()

        if (onlyTime) {
            // Solo selezione dell’ora
            TimePickerDialog(requireContext(), { _, hourOfDay, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                calendar.set(Calendar.MINUTE, minute)
                calendar.set(Calendar.SECOND, 0)

                // Format only time (es: 18:30)
                target.setText(String.format("%02d:%02d", hourOfDay, minute))
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
        } else {
            // Selezione data + ora
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
    }



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}