package com.example.travel_companion.presentation.ui.fragment

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.travel_companion.R
import com.example.travel_companion.data.local.entity.CoordinateEntity
import com.example.travel_companion.data.local.entity.POIEntity
import com.example.travel_companion.data.local.entity.TripEntity
import com.example.travel_companion.databinding.FragmentTripDetailBinding
import com.example.travel_companion.util.Utils
import com.example.travel_companion.presentation.viewmodel.TripDetailViewModel
import com.example.travel_companion.service.Polyline
import com.example.travel_companion.service.Polylines
import com.example.travel_companion.service.TrackingService
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import java.util.Date
import com.example.travel_companion.domain.model.TripStatus
import com.example.travel_companion.util.Utils.getFormattedTrackingTime
import com.google.android.gms.location.Geofence
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.PointOfInterest
import java.util.Locale

@AndroidEntryPoint
class TripDetailsFragment: Fragment() {
    private  var _binding: FragmentTripDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TripDetailViewModel by viewModels()
    private val args: TripDetailsFragmentArgs by navArgs()

    private var isTracking = TrackingService.isTracking.value ?: false
    private var pathPoints = mutableListOf<Polyline>()
    private var poiPoints = mutableListOf<POIEntity>()
    private var geofenceList = mutableListOf<Geofence>()
    private var map: GoogleMap? = null
    private var trackedDistance: MutableLiveData<Double?> = MutableLiveData(0.0)
    private var curTimeInMillis = 0L

    private var trackingObserver: Observer<Boolean>? = null
    private var pathPointsObserver: Observer<Polylines>? = null
    private var timerObserver: Observer<Long>? = null

    private var isFetching: MutableLiveData<Boolean> = MutableLiveData(true)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_trip_detail, container, false
        )

        binding.mapView.onCreate(savedInstanceState)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.mapView.onCreate(savedInstanceState)
        setupBottomNavigation()
        initTripData()
        subscribeToObservers()
        setupClickListeners()
    }

    /**
     * Initializes the Google Map with trip data and sets up map interactions.
     */
    private fun initializeMap(targetLocation: LatLng) {
        // only init map when the isFetching is turned to false
        isFetching.observe(viewLifecycleOwner) {
            binding.mapView.getMapAsync {
                map = it
                map!!.clear()

                addAllPolylines()

                for(poi in poiPoints) {
                    addPOIMarker(poi.name, LatLng(poi.latitude, poi.longitude))
                }

                if(pathPoints.isEmpty())
                    map!!.moveCamera(CameraUpdateFactory.newLatLngZoom(targetLocation, 15F))
                else
                    zoomToSeeWholeTrack()

                setMapClickListeners()
            }
        }
    }

    /**
     * Configures bottom navigation menu item selection and navigation actions.
     */
    private fun setupBottomNavigation() {
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.homeFragment -> {
                    findNavController().navigate(
                        TripDetailsFragmentDirections.actionTripFragmentToHomeFragment()
                    )
                    true
                }
                R.id.goToPhotoGallery -> {
                    findNavController().navigate(
                        TripDetailsFragmentDirections.actionTripDetailFragmentToPhotoGalleryFragment(args.tripId)
                    )
                    true
                }
                R.id.goToNotes -> {
                    findNavController().navigate(
                        TripDetailsFragmentDirections.actionTripDetailFragmentToNoteListFragment(args.tripId)
                    )
                    true
                }
                else -> false
            }
        }
    }

    /**
     * Sets up click listeners for tracking toggle and finish trip buttons.
     */
    private fun setupClickListeners() {
        binding.btnToggleTracking.setOnClickListener {
            toggleTracking()
        }

        binding.btnFinishTrip.setOnClickListener {
            showFinishTripDialog()
        }
    }

    /**
     * Displays confirmation dialog for finishing the trip.
     */
    private fun showFinishTripDialog() {
        val dialog = MaterialAlertDialogBuilder(requireContext(), R.style.Theme_ProvaProgetto_PopupOverlay)
            .setTitle("Terminazione viaggio")
            .setMessage("Terminando il viaggio non sarai più in grado di tracciare i tuoi spostamenti, sei sicuro di voler continuare?")
            .setPositiveButton("Si") { _, _ ->
                viewModel.finishTrip()
            }
            .setNegativeButton("No") { dialogInterface, _ ->
                dialogInterface.cancel()
            }
            .create()
        dialog.show()
    }

    /**
     * Loads and observes trip data including coordinates, POIs, and trip information.
     */
    @SuppressLint("SetTextI18n")
    private fun initTripData() {
        viewModel.loadCoordinates(args.tripId)
        viewModel.loadPOIs(args.tripId)
        viewModel.loadTrip(args.tripId)

        viewModel.coordinates.observe(viewLifecycleOwner) { coordinates ->
            coordinates.let {
                if (it.isNotEmpty()) {
                    initPathPoints(coordinates)
                }

                isFetching.postValue(false)
            }
        }

        viewModel.pois.observe(viewLifecycleOwner) { pois ->
            pois.let {
                if(it.isNotEmpty()) {
                    poiPoints = it.toMutableList()
                    populateGeofenceList(it)
                }
            }
        }

        viewModel.trip.observe(viewLifecycleOwner) { trip ->
            trip.let {
                trackedDistance.postValue(it!!.trackedDistance)
                TrackingService.trackingTimeInMillis.postValue(it.timeTracked)

                val destinationCoordinates = LatLng(it.destinationLatitude, it.destinationLongitude)
                initializeMap(destinationCoordinates)

                showTripInfo(it)
            }
        }

        trackedDistance.observe(viewLifecycleOwner) {
            if(trackedDistance.value!! >= 1000.00) {
                val distanceInKM = String.format(Locale.US, "%.2f", trackedDistance.value!! / 1000.0)
                binding.tvDistance.text = "$distanceInKM Km"
            } else {
                binding.tvDistance.text = "${trackedDistance.value!!.toInt()} m"
            }
        }
    }

    /**
     * Converts coordinate entities into polylines for map display, handling time gaps.
     */
    private fun initPathPoints(coordinates: List<CoordinateEntity>) {
        var previousTimestamp: Long = coordinates[0].timestamp
        var currentPolyline: Polyline = mutableListOf()
        var previousCoordinate = LatLng(coordinates[0].latitude, coordinates[0].longitude)

        for (coordinate in coordinates) {
            if((coordinate.timestamp - previousTimestamp) > (Utils.TRACKING_TIME * 10)) {
                pathPoints.add(currentPolyline)
                currentPolyline = mutableListOf() // reset current polyline
                pathPoints.add(mutableListOf()) // add empty polyline to separate next from the previous one
            }

            previousTimestamp = coordinate.timestamp
            previousCoordinate = LatLng(coordinate.latitude, coordinate.longitude)
            currentPolyline.add(previousCoordinate)
        }

        pathPoints.add(currentPolyline)
        pathPoints.add(mutableListOf())
    }

    /**
     * Sets up observers for tracking service state, path points, and timer updates.
     */
    private fun subscribeToObservers() {
        trackingObserver = Observer { isTracking ->
            if (isResumed) {
                updateTracking(isTracking)
            } else {
                this.isTracking = isTracking
            }
        }

        pathPointsObserver = Observer { pathPointsList ->
            pathPoints = pathPointsList

            refreshTrackedDistance()
            addNewCoordinate()

            // Only update UI if fragment is visible
            if (isResumed) {
                addLatestPolyline()
            }
        }

        timerObserver = Observer { time ->
            curTimeInMillis = time
            val formattedTime = getFormattedTrackingTime(curTimeInMillis)

            if(isResumed) {
                binding.tvTimer.text = formattedTime
            }
        }

        TrackingService.isTracking.observeForever(trackingObserver!!)
        TrackingService.pathPoints.observeForever(pathPointsObserver!!)
        TrackingService.trackingTimeInMillis.observeForever(timerObserver!!)
    }

    /**
     * Updates the total tracked distance based on new location points.
     */
    private fun refreshTrackedDistance() {
        if (pathPoints.isNotEmpty() && pathPoints.last().size > 1) {
            val secondLastPos = pathPoints.last()[pathPoints.last().size - 2]
            val lastPos = pathPoints.last().last()

            val result = FloatArray(1)
            Location.distanceBetween(
                secondLastPos.latitude,
                secondLastPos.longitude,
                lastPos.latitude,
                lastPos.longitude,
                result
            )

            val totalDistance = trackedDistance.value!! + result[0]
            trackedDistance.postValue(totalDistance)
        }
    }

    /**
     * Saves new coordinate points to the database during tracking.
     */
    private fun addNewCoordinate() {
        if(pathPoints.isNotEmpty() && pathPoints.last().isNotEmpty()) {
            val lat = pathPoints.last().last().latitude
            val long = pathPoints.last().last().longitude
            viewModel.insertCoordinate(lat, long, args.tripId)
        }
    }

    /**
     * Updates the UI with trip information and controls visibility based on trip status.
     */
    @SuppressLint("SetTextI18n")
    private fun showTripInfo(trip: TripEntity) {
        binding.tvDestination.text = trip.destination
        binding.tvStartDate.text = Utils.dateTimeFormat.format(Date(trip.startDate))
        binding.tvEndDate.text = Utils.dateTimeFormat.format(Date(trip.endDate))
        binding.tvDistance.text = trip.trackedDistance.toString()
        binding.tvStatus.text = trip.status.getValue()

        if (trip.status == TripStatus.FINISHED || trip.status == TripStatus.PLANNED) {
            binding.btnFinishTrip.visibility = View.GONE
            binding.btnToggleTracking.visibility = View.GONE
        } else {
            binding.btnToggleTracking.visibility = View.VISIBLE
            binding.btnFinishTrip.visibility = View.VISIBLE
        }

        // if is tracking and the scheduler terminates the trip, then stop the service
        if (isTracking && trip.status == TripStatus.FINISHED) {
            viewModel.updateTimeAndDistanceTracked(curTimeInMillis, trackedDistance.value!!)
            sendCommandToService("ACTION_STOP_SERVICE")
        }
    }

    /**
     * Toggles location tracking by starting or pausing the tracking service.
     */
    private fun toggleTracking() {
        if(!isTracking) {
            sendCommandToService("ACTION_START_OR_RESUME_SERVICE")
            TrackingService.geofenceList.postValue(geofenceList)
        } else {
            sendCommandToService("ACTION_PAUSE_SERVICE")
            viewModel.updateTimeAndDistanceTracked(curTimeInMillis, trackedDistance.value!!)
        }
    }

    /**
     * Updates UI elements and button states based on current tracking status.
     */
    @SuppressLint("SetTextI18n")
    private fun updateTracking(isTracking: Boolean) {
        this.isTracking = isTracking
        if(!isTracking) {
            binding.btnToggleTracking.text = "Traccia"
            binding.btnFinishTrip.visibility = View.VISIBLE
        } else if(isTracking && viewModel.trip.value!!.status == TripStatus.FINISHED){ // when the trip get finished by the alarm manager at the enddate
            binding.btnFinishTrip.visibility = View.GONE
            binding.btnToggleTracking.visibility = View.GONE
        } else {
            binding.btnToggleTracking.text = "Ferma"
            binding.btnFinishTrip.visibility = View.GONE
            updateLastTrackingTime()
        }
    }

    /**
     * Adjusts map camera to display the entire tracked path.
     */
    private fun zoomToSeeWholeTrack() {
        var hasPoints = false
        val bounds = LatLngBounds.Builder()

        for(polyline in pathPoints) {
            for(pos in polyline) {
                hasPoints = true
                bounds.include(pos)
            }
        }

        if(!hasPoints)
            return

        map?.moveCamera(
            CameraUpdateFactory.newLatLngBounds(
                bounds.build(),
                binding.mapView.width,
                binding.mapView.height,
                ( binding.mapView.height * 0.05f).toInt()
            )
        )
    }

    /**
     * Renders all tracked polylines on the map.
     */
    private fun addAllPolylines() {
        for(polyline in pathPoints) {
            val polylineOptions = PolylineOptions()
                .color(0xFF0000FF.toInt()) //blue
                .width(8f)
                .addAll(polyline)
            map?.addPolyline(polylineOptions)
        }
    }

    /**
     * Adds the most recent polyline segment to the map during active tracking.
     */
    private fun addLatestPolyline() {
        if(pathPoints.isNotEmpty() && pathPoints.last().size > 1) {
            val preLastLatLng = pathPoints.last()[pathPoints.last().size - 2] // second-last coordinate in the last polyline
            val lastLatLng = pathPoints.last().last() // last coordinate in the last polyline
            val polylineOptions = PolylineOptions()
                .color(0xFF0000FF.toInt()) //blue
                .width(8f)
                .add(preLastLatLng)
                .add(lastLatLng)
            map?.addPolyline(polylineOptions)
        }
    }

    /**
     * Stores the current timestamp for tracking session management.
     */
    private fun updateLastTrackingTime() {
        val sharedPrefs = requireContext().getSharedPreferences("app_tracking", Context.MODE_PRIVATE)
        sharedPrefs.edit()
            .putLong("last_tracking_time", System.currentTimeMillis())
            .apply()
    }

    /**
     * Sends action commands to the tracking service.
     */
    private fun sendCommandToService(action: String) =
        Intent(requireContext(), TrackingService::class.java).also {
            it.action = action
            requireContext().startService(it)
        }

    /**
     * Creates a geofence for a specific location with entry and exit monitoring.
     */
    private fun addGeofence(pos: LatLng, placeName: String) {
        val geofence = Geofence.Builder()
            .setRequestId(placeName)
            .setCircularRegion(
                pos.latitude,
                pos.longitude,
                100F //100 meters
            )
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
            .build()

        geofenceList.add(geofence)
    }

    /**
     * Generates geofences for all points of interest in the current trip.
     */
    private fun populateGeofenceList(pois: List<POIEntity>) {
        geofenceList.clear()

        for(poi in pois) {
            val pos = LatLng(poi.latitude, poi.longitude)
            addGeofence(pos, poi.name)
        }
    }

    /**
     * Configures map interaction listeners for POI creation and marker management.
     */
    @SuppressLint("PotentialBehaviorOverride")
    private fun setMapClickListeners() {
        map!!.setOnMapLongClickListener { latlng ->
            showPOIInputDialog(latlng)
        }

        map!!.setOnPoiClickListener { poi ->
            showAddPOIDialog(poi)
        }

        map!!.setOnMarkerClickListener { marker ->
            showMarkerDialog(marker)
            true // Consume the event
        }
    }

    /**
     * Displays dialog to save a Google Maps point of interest.
     */
    private fun showAddPOIDialog(poi: PointOfInterest) {
        AlertDialog.Builder(requireContext())
            .setTitle("Salva Punto di Interesse")
            .setMessage("Vuoi salvare il punto '${poi.name}'?")
            .setPositiveButton("Salva") { _, _ ->
                viewModel.insertPOI(args.tripId, poi.latLng, poi.name, poi.placeId)
                addGeofence(poi.latLng, poi.name)
                addPOIMarker(poi.name, poi.latLng)
                TrackingService.geofenceList.postValue(geofenceList) // post values to trigger update geofencing if active
            }
            .setNegativeButton("Annulla", null)
            .show()
    }

    /**
     * Shows input dialog for creating custom points of interest at map coordinates.
     */
    private fun showPOIInputDialog(latlng: LatLng) {
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
        }

        val nameInput = EditText(requireContext()).apply {
            hint = "Nome"
            inputType = InputType.TYPE_CLASS_TEXT
        }

        layout.addView(nameInput)

        val dialog = AlertDialog.Builder(requireContext()).setView(layout)
            .setTitle("Salva Punto di Interesse")
            .setPositiveButton("Salva",null)
            .setNegativeButton("Annulla", null)
            .create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val name = nameInput.text.toString().trim()
                if (name.isEmpty()) {
                    Toast.makeText(
                        requireContext(),
                        "Devi inserire un nome valido",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    viewModel.insertPOI(args.tripId, latlng, name, name)
                    addGeofence(latlng, name)
                    addPOIMarker(name, latlng)
                    TrackingService.geofenceList.postValue(geofenceList) // post values to trigger update geofencing if active
                    dialog.dismiss()
                }
            }
        }

        dialog.show()
    }

    /**
     * Places a point of interest marker on the map.
     */
    private fun addPOIMarker(poiName: String, poiPosition: LatLng) {
        map!!.addMarker(
            MarkerOptions()
                .position(poiPosition)
                .title("Punto di Interesse")
                .snippet(poiName)
        )
    }

    /**
     * Displays dialog for point of interest marker interactions and deletion.
     */
    private fun showMarkerDialog(marker: Marker) {
        AlertDialog.Builder(requireContext())
            .setTitle("Punto di Interesse")
            .setMessage(marker.snippet)
            .setPositiveButton("Elimina") { _, _ ->
                marker.remove()
                viewModel.deletePOI(marker.snippet!!, args.tripId)
                deleteGeofence(marker.snippet!!)
            }
            .setNegativeButton("Chiudi", null)
            .create()
            .show()
    }

    /**
     * Removes a geofence from the active monitoring list by name.
     */
    private fun deleteGeofence(poiName: String) {
        for(geofence in geofenceList) {
            if(geofence.requestId == poiName) {
                geofenceList.remove(geofence)
                TrackingService.geofenceList.postValue(geofenceList) //update geofencing if active
                break
            }
        }
    }

    override fun onResume() {
        super.onResume()
        _binding?.mapView?.onResume()

        addAllPolylines()
    }

    override fun onStart() {
        super.onStart()
        _binding?.mapView?.onStart()
    }

    override fun onStop() {
        super.onStop()
        _binding?.mapView?.onStop()
    }

    override fun onPause() {
        super.onPause()
        _binding?.mapView?.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        _binding?.mapView?.onSaveInstanceState(outState)
    }

    /**
     * Cleans up observers, stops tracking service, and destroys the view.
     */
    override fun onDestroyView() {
        trackingObserver?.let {
            TrackingService.isTracking.removeObserver(it)
        }
        pathPointsObserver?.let {
            TrackingService.pathPoints.removeObserver(it)
        }
        timerObserver?.let {
            TrackingService.trackingTimeInMillis.removeObserver(it)
        }

        sendCommandToService("ACTION_STOP_SERVICE")
        viewModel.updateTimeAndDistanceTracked(curTimeInMillis, trackedDistance.value!!)

        super.onDestroyView()
        _binding?.mapView?.onDestroy()
        _binding = null
    }
}