package com.example.travel_companion.presentation.ui.fragment

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Button
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.travel_companion.BaseApplication
import com.example.travel_companion.R
import com.example.travel_companion.databinding.FragmentNewTripBinding
import com.example.travel_companion.presentation.viewmodel.TripsViewModel
import com.google.android.gms.common.api.Status
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPhotoRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class NewTripFragment : Fragment() {

    private var _binding: FragmentNewTripBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TripsViewModel by viewModels()
    private var previousTripType: String? = null
    private lateinit var placesClient: PlacesClient

    private var latitude: Double = 0.0
    private var longitude: Double = 0.0

    /**
     * Inflates the layout for this fragment and initializes data binding.
     *
     * @param inflater LayoutInflater used to inflate views
     * @param container Optional parent view group
     * @param savedInstanceState Previously saved state
     * @return The root view of the fragment
     */
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

    /**
     * Called after the view is created. Initializes autocomplete, listeners, and observers.
     *
     * @param view The created view
     * @param savedInstanceState Previously saved state
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initPlacesAutocomplete()
        initListeners()
        observeViewModel()
    }

    /**
     * Initializes the Google Places autocomplete fragment and handles selection events.
     */
    private fun initPlacesAutocomplete() {
        val app = requireActivity().application as BaseApplication
        placesClient = app.placesClient

        val autocompleteFragment =
            childFragmentManager.findFragmentById(R.id.autocomplete_fragment) as AutocompleteSupportFragment

        autocompleteFragment.setPlaceFields(
            listOf(
                Place.Field.ID,
                Place.Field.DISPLAY_NAME,
                Place.Field.FORMATTED_ADDRESS,
                Place.Field.LOCATION,
                Place.Field.PHOTO_METADATAS
            )
        )

        autocompleteFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                val placeName = place.displayName ?: ""
                val placeAddress = place.formattedAddress ?: ""
                latitude = place.location?.latitude!!
                longitude = place.location?.longitude!!

                Timber.i("Luogo selezionato: $placeName, $placeAddress")
                viewModel.selectedDestinationName = placeName

                binding.textPlaceName.text = placeName
                loadPlacePhoto(place)
            }

            override fun onError(status: Status) {
                Timber.e("Errore Autocomplete: $status")
                Toast.makeText(
                    requireContext(),
                    "Errore: ${status.statusMessage}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    /**
     * Loads and displays a photo for the given place if available,
     * and stores it in the ViewModel.
     *
     * @param place The selected place
     */
    private fun loadPlacePhoto(place: Place) {
        val photoMetadatas = place.photoMetadatas
        if (photoMetadatas.isNullOrEmpty()) {
            Timber.d("Nessuna foto disponibile per questo luogo")
            try {
                binding.cardPlaceImage.visibility = View.GONE
            } catch (e: Exception) {
                Timber.d("cardPlaceImage non presente nel layout")
            }
            viewModel.selectedPlaceImageData = null
            return
        }

        val photoMetadata = photoMetadatas[0]

        val photoRequest = FetchPhotoRequest.builder(photoMetadata)
            .setMaxWidth(800)
            .setMaxHeight(600)
            .build()

        placesClient.fetchPhoto(photoRequest)
            .addOnSuccessListener { fetchPhotoResponse ->
                val bitmap: Bitmap = fetchPhotoResponse.bitmap

                try {
                    binding.imagePlace.setImageBitmap(bitmap)
                    binding.cardPlaceImage.visibility = View.VISIBLE
                } catch (e: Exception) {
                    Timber.d("Elementi UI per l'immagine non presenti nel layout, ma l'immagine è stata comunque salvata")
                }

                viewModel.setPlaceImage(bitmap)

                Timber.d("Foto del luogo caricata con successo")
            }
            .addOnFailureListener { exception: Exception ->
                Timber.e(exception, "Errore nel caricamento della foto")
                try {
                    binding.cardPlaceImage.visibility = View.GONE
                } catch (e: Exception) {
                    Timber.d("cardPlaceImage non presente nel layout")
                }
                viewModel.selectedPlaceImageData = null
            }
    }

    /**
     * Initializes listeners for date/time pickers, trip type spinner,
     * and trip creation button.
     */
    private fun initListeners() {
        binding.editStartDate.setOnClickListener {
            showDateTimePicker(binding.editStartDate, onlyTime = false)
        }

        binding.editEndDate.setOnClickListener {
            val type = binding.spinnerTripType.selectedItem.toString()
            val onlyTime = type == "Gita Giornaliera" || type == "Viaggio Locale"
            showDateTimePicker(binding.editEndDate, onlyTime)
        }

        binding.spinnerTripType.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val selected = parent.getItemAtPosition(position).toString()
                    binding.editEndDate.visibility = View.VISIBLE

                    when (selected) {
                        "Viaggio di più giorni" -> {
                            binding.editEndLabel.text = "Data e ora fine viaggio"
                            binding.editEndDate.visibility = View.VISIBLE
                            binding.editEndDate.text = "Seleziona data e ora"
                        }

                        "Gita Giornaliera", "Viaggio Locale" -> {
                            binding.editEndLabel.text = "Ora fine viaggio"
                            binding.editEndDate.visibility = View.VISIBLE
                            binding.editEndDate.text = "23:59"
                        }

                        else -> {
                            binding.editEndDate.visibility = View.GONE
                            binding.editEndDate.text = "Seleziona data e ora"
                        }
                    }

                    previousTripType = selected
                }

                override fun onNothingSelected(parent: AdapterView<*>) {}
            }

        binding.btnCreateTrip.setOnClickListener {
            viewModel.onCreateTripClicked(
                destination = viewModel.selectedDestinationName,
                startDateStr = binding.editStartDate.text.toString(),
                endDateStr = binding.editEndDate.text.toString(),
                type = binding.spinnerTripType.selectedItem.toString(),
                lat = latitude,
                long = longitude
            )
        }
    }

    /**
     * Observes LiveData from the ViewModel to handle UI events such as
     * showing messages and navigation.
     */
    private fun observeViewModel() {
        viewModel.uiEvent.observe(viewLifecycleOwner) { event ->
            when (event) {
                is TripsViewModel.Event.ShowMessage -> {
                    Toast.makeText(requireContext(), event.message, Toast.LENGTH_SHORT).show()
                }

                TripsViewModel.Event.Success -> {
                    findNavController().navigateUp()
                }
            }
        }
    }

    /**
     * Displays a date/time picker depending on the trip type.
     *
     * @param button The button to update with the result
     * @param onlyTime Whether only the time should be selected
     */
    @SuppressLint("DefaultLocale")
    private fun showDateTimePicker(button: Button, onlyTime: Boolean) {
        if (onlyTime) {
            showTimePicker { time ->
                button.text = time
            }
        } else {
            showDatePicker { date ->
                showTimePicker { time ->
                    button.text = "$date $time"
                }
            }
        }
    }

    /**
     * Displays a date picker dialog and invokes a callback with the selected date.
     *
     * @param onDateSelected Callback with formatted date string
     */
    private fun showDatePicker(onDateSelected: (String) -> Unit) {
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Seleziona data")
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            .build()

        datePicker.addOnPositiveButtonClickListener { dateInMillis ->
            val formattedDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(dateInMillis))
            onDateSelected(formattedDate)
        }

        datePicker.show(childFragmentManager, "DATE_PICKER")
    }

    /**
     * Displays a time picker dialog and invokes a callback with the selected time.
     *
     * @param onTimeSelected Callback with formatted time string
     */
    private fun showTimePicker(onTimeSelected: (String) -> Unit) {
        val timePicker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setTitleText("Seleziona ora")
            .build()

        timePicker.addOnPositiveButtonClickListener {
            val formattedTime = String.format(
                Locale.getDefault(),
                "%02d:%02d",
                timePicker.hour,
                timePicker.minute
            )
            onTimeSelected(formattedTime)
        }

        timePicker.show(childFragmentManager, "TIME_PICKER")
    }

    /**
     * Cleans up binding and resets ViewModel data when the view is destroyed.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.resetData()
        _binding = null
    }
}
