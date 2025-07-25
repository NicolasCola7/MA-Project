package com.example.travel_companion.presentation.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.graphics.Color
import androidx.core.view.MenuProvider
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.travel_companion.R
import com.example.travel_companion.databinding.FragmentTripDetailBinding
import com.example.travel_companion.domain.model.TripStatus
import com.example.travel_companion.presentation.viewmodel.TripDetailViewModel
import com.example.travel_companion.service.Polyline
import com.example.travel_companion.service.TrackingService
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TripDetailsFragment: Fragment() {
    private  var _binding: FragmentTripDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TripDetailViewModel by viewModels()
    private val args: NotesListFragmentArgs by navArgs()

    private var isTracking = false
    private var pathPoints = mutableListOf<Polyline>()
    private var map: GoogleMap? = null


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

        binding.btnStartTrip.setOnClickListener {
            startTracking()
        }

        binding.btnPauseTrip.setOnClickListener {
            startTracking()
        }

        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_trip_detail, menu)
            }

            //funzione richiamata quando si elimina un viaggio dall'icona nel menu
            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_delete -> {
                        showDeleteTripDialog()
                        true
                    }

                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)

        val tripId = args.tripId

        binding.mapView.getMapAsync {
            map = it
            addAllPolylines()
        }

        subscribeToObservers()
    }

    private fun subscribeToObservers() {
        TrackingService.isTracking.observe(viewLifecycleOwner) {
            updateTracking(it)
        }

        TrackingService.pathPoints.observe(viewLifecycleOwner) {
            pathPoints = it

            val lat = it.last().last().latitude
            val long = it.last().last().longitude
            viewModel.insertCoordinate(lat, long, args.tripId)

            addLatestPolyline()
            moveCameraToUser()
        }
    }

    private fun startTracking() {
        sendCommandToService("ACTION_START_OR_RESUME_SERVICE")
    }

    private fun pauseTracking() {
        sendCommandToService("ACTION_PAUSE_SERVICE")
    }

    private fun showDeleteTripDialog() {
        val dialog = MaterialAlertDialogBuilder(requireContext(), R.style.Theme_ProvaProgetto_PopupOverlay)
            .setTitle("Eliminazione viaggio")
            .setMessage("Sei sicuro di voler eliminare il viaggio e tutti i dati relativi ad esso?")
            .setPositiveButton("Si") { _, _ ->
                deleteTrip()
            }
            .setNegativeButton("No") { dialogInterface, _ ->
                dialogInterface.cancel()
            }
            .create()
        dialog.show()
    }

    private fun deleteTrip() {
        viewModel.deleteTrip()
        findNavController().navigate(R.id.action_tripDetailFragment_to_homeFragment)
    }

    private fun updateTracking(isTracking: Boolean) {
        this.isTracking = isTracking
        if(!isTracking) {
            viewModel.updateTripStatus(TripStatus.PAUSED)
        } else {
            viewModel.updateTripStatus(TripStatus.STARTED)
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

    private fun sendCommandToService(action: String) =
        Intent(requireContext(), TrackingService::class.java).also {
            it.action = action
            requireContext().startService(it)
        }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onStart() {
        super.onStart()
        binding.mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        binding.mapView.onStop()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.mapView.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        pauseTracking()
        super.onDestroyView()
        _binding = null
    }
}