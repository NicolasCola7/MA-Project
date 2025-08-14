package com.example.travel_companion.presentation.ui.fragment

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
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
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.TileOverlay
import com.google.android.gms.maps.model.TileOverlayOptions
import com.google.maps.android.heatmaps.HeatmapTileProvider
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class StatisticsFragment : Fragment() {

    private var _binding: FragmentStatisticsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: StatisticsViewModel by viewModels()

    private var googleMap: GoogleMap? = null
    private var heatmapTileOverlay: TileOverlay? = null

    // Variabile per tracciare la vista corrente
    private var isMapViewVisible = true

    // Cache dei dati per ripristinare la heatmap
    private var cachedTrips: List<com.example.travel_companion.data.local.entity.TripEntity> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatisticsBinding.inflate(inflater, container, false)
        binding.mapView.onCreate(savedInstanceState)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupMapView(savedInstanceState)
        setupToggleButtons()
        setupObservers()
        setupBarChart()

        // Inizializza con la vista mappa
        showMapView()

        viewModel.loadStatistics()
    }

    private fun setupToggleButtons() {
        binding.btnMap.setOnClickListener {
            if (!isMapViewVisible) {
                showMapView()
            }
        }

        binding.btnStats.setOnClickListener {
            if (isMapViewVisible) {
                showStatsView()
            }
        }
    }

    private fun showMapView() {
        isMapViewVisible = true

        // Mostra mappa, nascondi statistiche
        binding.mapView.visibility = View.VISIBLE
        binding.statsContainer.visibility = View.GONE

        // Aggiorna stili dei pulsanti
        updateButtonStyles(true)
    }

    private fun showStatsView() {
        isMapViewVisible = false

        // Nascondi mappa, mostra statistiche
        binding.mapView.visibility = View.GONE
        binding.statsContainer.visibility = View.VISIBLE

        // Aggiorna stili dei pulsanti
        updateButtonStyles(false)
    }

    private fun updateButtonStyles(mapSelected: Boolean) {
        if (mapSelected) {
            // Pulsante mappa selezionato
            binding.btnMap.setBackgroundResource(R.drawable.toggle_button_selected)
            binding.btnMap.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))

            // Pulsante statistiche non selezionato
            binding.btnStats.setBackgroundResource(R.drawable.toggle_button_unselected)
            binding.btnStats.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
        } else {
            // Pulsante mappa non selezionato
            binding.btnMap.setBackgroundResource(R.drawable.toggle_button_unselected)
            binding.btnMap.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))

            // Pulsante statistiche selezionato
            binding.btnStats.setBackgroundResource(R.drawable.toggle_button_selected)
            binding.btnStats.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
        }
    }

    private fun setupMapView(savedInstanceState: Bundle?) {
        binding.mapView.getMapAsync { map ->
            googleMap = map
            // Riapplica la heatmap se abbiamo dati cached
            if (cachedTrips.isNotEmpty()) {
                updateHeatmap(cachedTrips)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        _binding?.mapView?.onResume()
        // Ricarica i dati quando il fragment torna visibile
        viewModel.loadStatistics()
    }

    override fun onPause() {
        super.onPause()
        _binding?.mapView?.onPause()
    }

    override fun onStart() {
        super.onStart()
        _binding?.mapView?.onStart()
    }

    override fun onStop() {
        super.onStop()
        _binding?.mapView?.onStop()
    }

    private fun setupObservers() {
        // Osserva StateFlow dal ViewModel
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.completedTrips.collect { trips ->
                // Salva i dati nella cache
                cachedTrips = trips

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
        _binding?.mapView?.onSaveInstanceState(outState)

        // Salva lo stato corrente della vista
        outState.putBoolean("is_map_view_visible", isMapViewVisible)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)

        // Ripristina lo stato della vista se disponibile
        savedInstanceState?.let { bundle ->
            isMapViewVisible = bundle.getBoolean("is_map_view_visible", true)
            if (isMapViewVisible) {
                showMapView()
            } else {
                showStatsView()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Pulisci le risorse della mappa
        heatmapTileOverlay?.remove()
        heatmapTileOverlay = null
        googleMap = null
        _binding?.mapView?.onDestroy()
        _binding = null
    }
}