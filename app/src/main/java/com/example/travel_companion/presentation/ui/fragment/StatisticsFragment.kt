package com.example.travel_companion.presentation.ui.fragment

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.travel_companion.R
import com.example.travel_companion.databinding.FragmentStatisticsBinding
import com.example.travel_companion.domain.model.TripStatus
import com.example.travel_companion.presentation.viewmodel.StatisticsViewModel
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.TileOverlay
import com.google.android.gms.maps.model.TileOverlayOptions
import com.google.maps.android.heatmaps.HeatmapTileProvider
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class StatisticsFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentStatisticsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: StatisticsViewModel by viewModels()

    private var mapView: MapView? = null
    private var googleMap: GoogleMap? = null
    private var heatmapTileOverlay: TileOverlay? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatisticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupMapView(savedInstanceState)
        setupObservers()
        setupBarChart()

        viewModel.loadStatistics()
    }

    private fun setupMapView(savedInstanceState: Bundle?) {
        try {
            mapView = binding.mapView

            mapView?.let { mv ->
                mv.onCreate(savedInstanceState)
                mv.getMapAsync { map ->
                    onMapReady(map)
                }
            }

        } catch (e: Exception) {
            Timber.tag("StatisticsFragment").e(e, "Errore nell'inizializzazione MapView")
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            mapView?.onResume()
        } catch (e: Exception) {
            Timber.tag("StatisticsFragment").e(e, "Errore in onResume MapView")
        }
        // Ricarica i dati quando il fragment torna visibile
        viewModel.loadStatistics()
    }

    override fun onPause() {
        super.onPause()
        try {
            mapView?.onPause()
        } catch (e: Exception) {
            Timber.tag("StatisticsFragment").e(e, "Errore in onPause MapView")
        }
    }

    override fun onStart() {
        super.onStart()
        try {
            mapView?.onStart()
        } catch (e: Exception) {
            Timber.tag("StatisticsFragment").e(e, "Errore in onStart MapView")
        }
    }

    override fun onStop() {
        super.onStop()
        try {
            mapView?.onStop()
        } catch (e: Exception) {
            Timber.tag("StatisticsFragment").e(e, "Errore in onStop MapView")
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView?.onLowMemory()
    }

    private fun setupObservers() {
        // Osserva StateFlow dal ViewModel
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.completedTrips.collect { trips ->
                updateHeatmap(trips)
                updateMonthlyChart(trips)
                updateStatisticsCards(trips)
            }
        }
    }

    private fun setupBarChart() {
        binding.monthlyChart.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(false)
            setDrawBarShadow(false)
            setDrawValueAboveBar(true)

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                labelCount = 12
            }

            axisLeft.apply {
                setDrawGridLines(true)
                axisMinimum = 0f
            }

            axisRight.isEnabled = false
            legend.isEnabled = false
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        googleMap?.apply {
            uiSettings.isZoomControlsEnabled = false 
            uiSettings.isMapToolbarEnabled = false
            uiSettings.isMyLocationButtonEnabled = false

            // Abilita tutti i gesti per una navigazione fluida
            uiSettings.isZoomGesturesEnabled = true
            uiSettings.isScrollGesturesEnabled = true
            uiSettings.isRotateGesturesEnabled = true
            uiSettings.isTiltGesturesEnabled = true

            // Centra la mappa sull'Italia come posizione iniziale
            val italyCenter = LatLng(41.8719, 12.5674)
            moveCamera(CameraUpdateFactory.newLatLngZoom(italyCenter, 5f))
        }

        // Ricarica i dati quando la mappa è pronta
        val currentTrips = viewModel.completedTrips.value
        if (currentTrips.isNotEmpty()) {
            updateHeatmap(currentTrips)
        }
    }

    private fun updateHeatmap(trips: List<com.example.travel_companion.data.local.entity.TripEntity>) {
        googleMap?.let { map ->
            // Rimuovi tutti i marker esistenti e la heatmap
            map.clear()
            heatmapTileOverlay?.remove()
            heatmapTileOverlay = null

            if (trips.isNotEmpty()) {
                val heatmapData = trips.map { trip ->
                    LatLng(trip.destinationLatitude, trip.destinationLongitude)
                }

                // Crea il provider della heatmap
                val heatmapProvider = HeatmapTileProvider.Builder()
                    .data(heatmapData)
                    .radius(50)
                    .opacity(0.7)
                    .build()

                // Aggiungi la heatmap alla mappa
                heatmapTileOverlay = map.addTileOverlay(
                    TileOverlayOptions().tileProvider(heatmapProvider)
                )

                // Aggiungi anche dei marker per i singoli viaggi
                trips.forEach { trip ->
                    val position = LatLng(trip.destinationLatitude, trip.destinationLongitude)
                    map.addMarker(
                        MarkerOptions()
                            .position(position)
                            .title(trip.destination)
                            .snippet("${trip.type} - ${formatDate(trip.startDate)}")
                    )
                }

                // Centra la mappa sui viaggi se ne abbiamo almeno uno
                if (heatmapData.isNotEmpty()) {
                    val bounds = com.google.android.gms.maps.model.LatLngBounds.builder()
                    heatmapData.forEach { bounds.include(it) }
                    try {
                        map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), 100))
                    } catch (e: Exception) {
                        // Se ci sono problemi con i bounds, centra sul primo punto
                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(heatmapData.first(), 10f))
                    }
                }
            }
        }
    }

    private fun updateMonthlyChart(trips: List<com.example.travel_companion.data.local.entity.TripEntity>) {
        val monthlyData = calculateMonthlyTrips(trips)
        val entries = monthlyData.mapIndexed { index, count ->
            BarEntry(index.toFloat(), count.toFloat())
        }

        val dataSet = BarDataSet(entries, "Viaggi per Mese").apply {
            color = Color.parseColor("#2196F3")
            valueTextColor = Color.BLACK
            valueTextSize = 12f
        }

        val barData = BarData(dataSet)
        barData.barWidth = 0.8f

        binding.monthlyChart.apply {
            data = barData
            xAxis.valueFormatter = IndexAxisValueFormatter(getMonthLabels())
            animateY(1000)
            invalidate()
        }
    }

    @SuppressLint("SetTextI18n", "DefaultLocale")
    private fun updateStatisticsCards(trips: List<com.example.travel_companion.data.local.entity.TripEntity>) {
        val completedTrips = trips.filter { it.status == TripStatus.FINISHED }
        val totalDistanceInMeters = completedTrips.sumOf { it.trackedDistance }
        val totalDistanceInKm = totalDistanceInMeters / 1000.0
        val uniqueDestinations = completedTrips.map { it.destination }.distinct().size

        binding.apply {
            totalTripsText.text = completedTrips.size.toString()

            // Mostra in metri se < 1000m, altrimenti in km
            totalDistanceText.text = if (totalDistanceInMeters < 1000) {
                String.format("%.0f m", totalDistanceInMeters)
            } else {
                String.format("%.1f km", totalDistanceInKm)
            }

            uniqueDestinationsText.text = uniqueDestinations.toString()

            // Calcola il viaggio più lungo
            val longestTrip = completedTrips.maxByOrNull { it.trackedDistance }
            longestTripText.text = longestTrip?.destination ?: "N/A"
        }
    }

    private fun calculateMonthlyTrips(trips: List<com.example.travel_companion.data.local.entity.TripEntity>): IntArray {
        val monthlyCount = IntArray(12)
        val calendar = Calendar.getInstance()

        trips.filter { it.status == TripStatus.FINISHED }.forEach { trip ->
            calendar.timeInMillis = trip.startDate
            val month = calendar.get(Calendar.MONTH)
            monthlyCount[month]++
        }

        return monthlyCount
    }

    private fun getMonthLabels(): Array<String> {
        return arrayOf(
            "Gen", "Feb", "Mar", "Apr", "Mag", "Giu",
            "Lug", "Ago", "Set", "Ott", "Nov", "Dic"
        )
    }

    private fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView?.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Pulisci le risorse della mappa
        heatmapTileOverlay?.remove()
        heatmapTileOverlay = null
        googleMap = null
        mapView?.onDestroy()
        mapView = null
        _binding = null
    }
}