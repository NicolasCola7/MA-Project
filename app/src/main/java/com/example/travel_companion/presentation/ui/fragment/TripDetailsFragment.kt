package com.example.travel_companion.presentation.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.travel_companion.R
import com.example.travel_companion.data.local.entity.CoordinateEntity
import com.example.travel_companion.databinding.FragmentTripDetailBinding
import com.example.travel_companion.presentation.Utils
import com.example.travel_companion.presentation.viewmodel.TripDetailViewModel
import com.example.travel_companion.service.Polyline
import com.example.travel_companion.service.Polylines
import com.example.travel_companion.service.TrackingService
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.PolylineOptions
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TripDetailsFragment: Fragment() {
    private  var _binding: FragmentTripDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TripDetailViewModel by viewModels()
    private val args: TripDetailsFragmentArgs by navArgs()

    private var isTracking = false
    private var pathPoints = mutableListOf<Polyline>()
    private var map: GoogleMap? = null

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
        setupClickListeners()
        initializeMap()
        subscribeToObservers()

    }

    private fun initializeMap() {
        binding.mapView.getMapAsync {
            map = it
            addAllPolylines()
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
    }

    private fun initTripData() {
        viewModel.loadTrip(args.tripId)
        viewModel.loadCoordinates(args.tripId)

        viewModel.trip.observe(viewLifecycleOwner) {
            trip -> trip.let {

                //showTripInfo(it)
            }
        }

        viewModel.coordinates.observe(viewLifecycleOwner) {
            coordinates -> coordinates.let {
                if(it.isNotEmpty()) {
                    initPathPoints(coordinates)
                }
            }
        }

    }

    private fun initPathPoints(coordinates: List<CoordinateEntity>) {
        var previousTimestamp: Long = coordinates[0].timestamp
        var currentPolyline: Polyline = mutableListOf()

        for (coordinate in coordinates) {
            if((coordinate.timestamp - previousTimestamp) > (Utils.TRACKING_TIME * 2)) {
                pathPoints.add(currentPolyline)
                currentPolyline = mutableListOf() // reset current polyline
                pathPoints.add(mutableListOf()) // add empty polyline to separate next from the previous one
                //Timber.d("New polylyne detected")
            }

            val pos = LatLng(coordinate.latitude, coordinate.longitude)
            currentPolyline.add(pos)
          //Timber.d("Add coord to current polyline --> " + "Lat: " + coordinate.latitude +"; Long: " + coordinate.longitude + "; timestamp: " + Utils.dateTimeFormat.format(Date(coordinate.timestamp)))
            previousTimestamp = coordinate.timestamp
        }

        pathPoints.add(currentPolyline)
        pathPoints.add(mutableListOf())
        addAllPolylines()
        zoomToSeeWholeTrack()
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

            if(pathPointsList.isNotEmpty() && pathPointsList.last().isNotEmpty()) {
                val lat = pathPointsList.last().last().latitude
                val long = pathPointsList.last().last().longitude
                viewModel.insertCoordinate(lat, long, args.tripId)
                //Timber.d("stored")
            }

            // Only update UI if fragment is visible
            if (isResumed) {
                addLatestPolyline()
            }
        }

        TrackingService.isTracking.observeForever(trackingObserver!!)
        TrackingService.pathPoints.observeForever(pathPointsObserver!!)
    }

    /*
    private fun showTripInfo(trip: TripEntity) {
        binding.tvDestination.text = trip.destination
        binding.tvType.text = trip.type
        binding.tvDates.text = "${Utils.dateTimeFormat.format(Date(trip.startDate))} - ${
            trip.endDate?.let { Utils.dateTimeFormat.format(Date(it)) } ?: "—"
        }"
        binding.tvStatus.text = "Stato: ${trip.status.name}"
    }*/

    private fun toggleTracking() {
        if(!isTracking) {
            sendCommandToService("ACTION_START_OR_RESUME_SERVICE")
        } else {
            sendCommandToService("ACTION_PAUSE_SERVICE")
            // saveTrackingProgress()
            zoomToSeeWholeTrack()
        }
    }

    private fun updateTracking(isTracking: Boolean) {
        this.isTracking = isTracking
        if(!isTracking) {
            binding.btnToggleTracking.text = "Start"
        } else {
            binding.btnToggleTracking.text = "Stop"
        }
    }

    private fun moveCameraToUser() {
        if(pathPoints.isNotEmpty() && pathPoints.last().isNotEmpty()) {
            map?.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    pathPoints.last().last(),
                    15f //map's zoom
                )
            )
        }
    }

    private fun zoomToSeeWholeTrack() {
        if(pathPoints.isEmpty())
            return

        val bounds = LatLngBounds.Builder()
        for(polyline in pathPoints) {
            for(pos in polyline) {
                bounds.include(pos)
            }
        }

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

    /*
    private fun saveTrackingProgress() {
        for(polyline in pathPoints) {
            distanceInMeters += Utils.calculatePolylineLength(polyline).toInt()
        }
    } */

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
        //saveTrackingProgress()
        super.onDestroyView()
        _binding = null
    }
}