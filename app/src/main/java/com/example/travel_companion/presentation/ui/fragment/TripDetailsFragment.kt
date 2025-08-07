package com.example.travel_companion.presentation.ui.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.text.font.SystemFontFamily
import androidx.core.view.MenuProvider
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.travel_companion.R
import com.example.travel_companion.data.local.entity.CoordinateEntity
import com.example.travel_companion.data.local.entity.TripEntity
import com.example.travel_companion.databinding.FragmentTripDetailBinding
import com.example.travel_companion.domain.model.TripStatus
import com.example.travel_companion.presentation.Utils
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

@AndroidEntryPoint
class TripDetailsFragment: Fragment() {
    private  var _binding: FragmentTripDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TripDetailViewModel by viewModels()
    private val args: TripDetailsFragmentArgs by navArgs()

    private var isTracking = false
    private var pathPoints = mutableListOf<Polyline>()
    private var map: GoogleMap? = null
    private var trackedDistance: MutableLiveData<Double> = MutableLiveData(0.0)
    private var stopPoints: MutableList<LatLng> = mutableListOf()

    private var trackingObserver: Observer<Boolean>? = null
    private var pathPointsObserver: Observer<Polylines>? = null

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

    private fun initializeMap(targetLocation: LatLng) {
        binding.mapView.getMapAsync {
            map = it
            addAllPolylines()

            for(point in stopPoints) {
                addStopMarker(point)
            }

            if(pathPoints.isEmpty())
                map!!.moveCamera(CameraUpdateFactory.newLatLngZoom(targetLocation, 15F))
            else
                zoomToSeeWholeTrack()
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
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

    private fun setupClickListeners() {
        binding.btnToggleTracking.setOnClickListener {
            toggleTracking()
        }

        binding.btnFinishTrip.setOnClickListener {
            showFinishTripDialog()
        }
    }

    private fun showFinishTripDialog() {
        val dialog = MaterialAlertDialogBuilder(requireContext(), R.style.Theme_ProvaProgetto_PopupOverlay)
            .setTitle("Terminazione viaggio")
            .setMessage("terminando il viaggio non sarai più in grado di tracciare i tuoi spostament, sei sicuro di voler continuare?")
            .setPositiveButton("Si") { _, _ ->
                finishTrip()
            }
            .setNegativeButton("No") { dialogInterface, _ ->
                dialogInterface.cancel()
            }
            .create()
        dialog.show()
    }

    private fun finishTrip() {
        sendCommandToService("ACTION_STOP_SERVICE")
        viewModel.updateTripStatus(TripStatus.FINISHED)

        binding.btnFinishTrip.visibility = View.GONE
        binding.btnToggleTracking.visibility = View.GONE

        viewModel.updateTripDistance(trackedDistance.value!!)
    }

    private fun initTripData() {
        viewModel.loadTrip(args.tripId)
        viewModel.loadCoordinates(args.tripId)

        viewModel.coordinates.observe(viewLifecycleOwner) { coordinates ->
            coordinates.let {
                if (it.isNotEmpty()) {
                    initPathPoints(coordinates)
                }
            }
        }

        viewModel.trip.observe(viewLifecycleOwner) { trip ->
            trip.let {
                trackedDistance.postValue(it!!.trackedDistance)

                val destinationCoordinates = LatLng(it.destinationLatitude, it.destinationLongitude)
                initializeMap(destinationCoordinates)

                showTripInfo(it)
            }
        }

        trackedDistance.observe(viewLifecycleOwner) {
            if(trackedDistance.value!! >= 1000.00) {
                val distanceInKM = trackedDistance.value!! / 1000
                binding.tvDistance.text = distanceInKM.toString() + " Km"
            } else {
                binding.tvDistance.text = trackedDistance.value!!.toInt().toString() + " m"
            }
        }
    }

    private fun initPathPoints(coordinates: List<CoordinateEntity>) {
        var previousTimestamp: Long = coordinates[0].timestamp
        var currentPolyline: Polyline = mutableListOf()
        var previousCoordinate = LatLng(coordinates[0].latitude, coordinates[0].longitude)

        for (coordinate in coordinates) {
            if((coordinate.timestamp - previousTimestamp) > (Utils.TRACKING_TIME * 2)) {
                pathPoints.add(currentPolyline)
                currentPolyline = mutableListOf() // reset current polyline
                pathPoints.add(mutableListOf()) // add empty polyline to separate next from the previous one
            }

            if((coordinate.timestamp - previousTimestamp) > 1000000) {
                stopPoints.add(previousCoordinate)
            }

            previousTimestamp = coordinate.timestamp
            previousCoordinate =  LatLng(coordinate.latitude, coordinate.longitude)
            currentPolyline.add(previousCoordinate)
        }

        pathPoints.add(currentPolyline)
        pathPoints.add(mutableListOf())
    }

    private fun subscribeToObservers() {
        trackingObserver = Observer<Boolean> { isTracking ->
            if (isResumed) {
                updateTracking(isTracking)
            } else {
                this.isTracking = isTracking
            }
        }

        pathPointsObserver = Observer<Polylines> { pathPointsList ->
            pathPoints = pathPointsList

            refreshTrackedDistance()
            addNewCoordinate()

            // Only update UI if fragment is visible
            if (isResumed) {
                addLatestPolyline()
            }
        }

        TrackingService.isTracking.observeForever(trackingObserver!!)
        TrackingService.pathPoints.observeForever(pathPointsObserver!!)
    }
    
    private fun refreshTrackedDistance() {
        if (pathPoints.isNotEmpty()) {
            var totalDistance = 0.0
            for (polyline in pathPoints) {
                totalDistance += Utils.calculatePolylineLength(polyline)
            }
            trackedDistance.postValue(totalDistance)
        }
    }
    
    private fun addNewCoordinate() {
        if(pathPoints.isNotEmpty() && pathPoints.last().isNotEmpty()) {
            val lat = pathPoints.last().last().latitude
            val long = pathPoints.last().last().longitude
            viewModel.insertCoordinate(lat, long, args.tripId)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun showTripInfo(trip: TripEntity) {
        binding.tvDestination.text = trip.destination
        binding.tvStartDate.text = Utils.dateTimeFormat.format(Date(trip.startDate))
        binding.tvEndDate.text = Utils.dateTimeFormat.format(Date(trip.endDate))
        binding.tvDistance.text = trip.trackedDistance.toString()
        binding.tvStatus.text = trip.status.getValue()

        if(trip.startDate > System.currentTimeMillis() || trip.endDate < System.currentTimeMillis()) {
            binding.btnToggleTracking.visibility = View.GONE
            binding.btnFinishTrip.visibility = View.GONE
        }
    }

    private fun toggleTracking() {
        if(!isTracking) {
            sendCommandToService("ACTION_START_OR_RESUME_SERVICE")
        } else {
            sendCommandToService("ACTION_PAUSE_SERVICE")
            viewModel.updateTripDistance(trackedDistance.value!!)
        }
    }

    private fun updateTracking(isTracking: Boolean) {
        this.isTracking = isTracking
        if(!isTracking) {
            binding.btnToggleTracking.text = "Start"
            binding.btnFinishTrip.visibility = View.VISIBLE
        } else {
            binding.btnToggleTracking.text = "Stop"
            binding.btnFinishTrip.visibility = View.GONE
        }
    }

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

    private fun addAllPolylines() {
        for(polyline in pathPoints) {
            val polylineOptions = PolylineOptions()
                .color(0xFF0000FF.toInt()) //blue
                .width(8f)
                .addAll(polyline)
            map?.addPolyline(polylineOptions)
        }
    }

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

    private fun addStopMarker(coordinates: LatLng) {
        map?.addMarker(
            MarkerOptions()
                .position(coordinates)
                .title("Fermata")
        )
    }

    private fun sendCommandToService(action: String) =
        Intent(requireContext(), TrackingService::class.java).also {
            it.action = action
            requireContext().startService(it)
        }

    override fun onResume() {
        super.onResume()
        _binding?.mapView?.onResume()

        updateTracking(isTracking)
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

    override fun onDestroyView() {
        //remove observers
        trackingObserver?.let {
            TrackingService.isTracking.removeObserver(it)
        }
        pathPointsObserver?.let {
            TrackingService.pathPoints.removeObserver(it)
        }

        sendCommandToService("ACTION_STOP_SERVICE")
        viewModel.updateTripDistance(trackedDistance.value!!)
        super.onDestroyView()
        _binding = null
    }
}