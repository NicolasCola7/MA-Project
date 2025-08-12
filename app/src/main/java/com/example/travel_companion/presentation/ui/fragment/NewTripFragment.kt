package com.example.travel_companion.presentation.ui.fragment

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.graphics.Bitmap
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
import com.example.travel_companion.BaseApplication
import com.example.travel_companion.R
import com.example.travel_companion.databinding.FragmentNewTripBinding
import com.example.travel_companion.util.Utils
import com.example.travel_companion.presentation.viewmodel.TripsViewModel
import com.example.travel_companion.util.PermissionsManager
import com.google.android.gms.common.api.Status
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPhotoRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
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

        initPlacesAutocomplete()
        initListeners()
        observeViewModel()
    }

    //inizializzazione fragment google
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

    private fun loadPlacePhoto(place: Place) {
        val photoMetadatas = place.photoMetadatas
        if (photoMetadatas.isNullOrEmpty()) {
            Timber.d("Nessuna foto disponibile per questo luogo")
            // Gestisci il caso in cui gli elementi UI non esistono ancora
            try {
                binding.cardPlaceImage.visibility = View.GONE
            } catch (e: Exception) {
                Timber.d("cardPlaceImage non presente nel layout")
            }
            viewModel.selectedPlaceImageData = null
            return
        }

        // Prendi la prima foto disponibile
        val photoMetadata = photoMetadatas[0]

        // Crea la richiesta per scaricare la foto
        val photoRequest = FetchPhotoRequest.builder(photoMetadata)
            .setMaxWidth(800) // Larghezza massima in pixel
            .setMaxHeight(600) // Altezza massima in pixel
            .build()

        placesClient.fetchPhoto(photoRequest)
            .addOnSuccessListener { fetchPhotoResponse ->
                val bitmap: Bitmap = fetchPhotoResponse.bitmap

                // Gestisci il caso in cui gli elementi UI potrebbero non esistere
                try {
                    binding.imagePlace.setImageBitmap(bitmap)
                    binding.cardPlaceImage.visibility = View.VISIBLE
                } catch (e: Exception) {
                    Timber.d("Elementi UI per l'immagine non presenti nel layout, ma l'immagine è stata comunque salvata")
                }

                // Salva l'immagine nel ViewModel per poi salvarla nel database
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

    //inizializza i listener degli altri elementi del fragment
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
                            binding.editEndLayout.hint = "Data e ora fine viaggio"
                            if (previousTripType != "Viaggio di più giorni") {
                                binding.editEndDate.setText("")
                            }
                        }

                        "Gita Giornaliera", "Viaggio Locale" -> {
                            binding.editEndLayout.hint = "Ora fine viaggio"
                            if (previousTripType == "Viaggio di più giorni") {
                                binding.editEndDate.setText("")
                            }
                        }

                        else -> {
                            binding.editEndDate.visibility = View.GONE
                            binding.editEndDate.setText("")
                        }
                    }

                    previousTripType = selected
                }

                override fun onNothingSelected(parent: AdapterView<*>) {}
            }

        binding.btnCreateTrip.setOnClickListener {
            PermissionsManager.checkExactAlarmPermissionSmart(requireActivity()) {
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
    }

    //osserva i dati dal view model
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

    //imposta lo stile del picker a seconda del tipo di viaggio
    @SuppressLint("DefaultLocale")
    private fun showDateTimePicker(target: EditText, onlyTime: Boolean = false) {
        val calendar = Calendar.getInstance()

        if (onlyTime) {
            TimePickerDialog(
                requireContext(),
                { _, hourOfDay, minute ->
                    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                    calendar.set(Calendar.MINUTE, minute)
                    calendar.set(Calendar.SECOND, 0)
                    target.setText(String.format("%02d:%02d", hourOfDay, minute))
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
            ).show()
        } else {
            DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    calendar.set(Calendar.YEAR, year)
                    calendar.set(Calendar.MONTH, month)
                    calendar.set(Calendar.DAY_OF_MONTH, day)

                    TimePickerDialog(
                        requireContext(),
                        { _, hourOfDay, minute ->
                            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                            calendar.set(Calendar.MINUTE, minute)
                            calendar.set(Calendar.SECOND, 0)
                            target.setText(Utils.dateTimeFormat.format(calendar.time))
                        },
                        calendar.get(Calendar.HOUR_OF_DAY),
                        calendar.get(Calendar.MINUTE),
                        true
                    ).show()
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Reset dei dati quando si esce dal fragment
        viewModel.resetData()
        _binding = null
    }

}