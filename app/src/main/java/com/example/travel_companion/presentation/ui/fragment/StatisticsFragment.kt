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
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.travel_companion.R
import com.example.travel_companion.data.local.entity.TripEntity
import com.example.travel_companion.databinding.FragmentStatisticsBinding
import com.example.travel_companion.domain.model.TripStatus
import com.example.travel_companion.presentation.viewmodel.StatisticsViewModel
import com.example.travel_companion.presentation.viewmodel.PredictionViewModel
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.TileOverlay
import com.google.android.gms.maps.model.TileOverlayOptions
import com.google.android.material.snackbar.Snackbar
import com.google.maps.android.heatmaps.HeatmapTileProvider
import com.google.maps.android.heatmaps.Gradient
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class StatisticsFragment : Fragment() {

    private var _binding: FragmentStatisticsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: StatisticsViewModel by viewModels()
    private val predictionViewModel: PredictionViewModel by viewModels()

    private var googleMap: GoogleMap? = null
    private var heatmapTileOverlay: TileOverlay? = null

    private var currentView = ViewType.MAP

    // Cache dei dati per ripristinare la heatmap
    private var cachedTrips: List<TripEntity> = emptyList()

    enum class ViewType { MAP, STATS, PREDICTIONS }

    companion object {
        private const val TAG = "StatisticsFragment"
    }

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

        setupMapView()
        setupToggleButtons()
        setupObservers()
        setupBarChart()

        // Inizializza con la vista mappa
        showView(ViewType.MAP)

        viewModel.loadStatistics()
        predictionViewModel.loadPredictions()
    }

    private fun setupToggleButtons() {
        binding.btnMap.setOnClickListener {
            if (currentView != ViewType.MAP) {
                showView(ViewType.MAP)
            }
        }

        binding.btnStats.setOnClickListener {
            if (currentView != ViewType.STATS) {
                showView(ViewType.STATS)
            }
        }

        // AGGIUNTO: Handler per il pulsante Previsioni
        binding.btnPredictions.setOnClickListener {
            if (currentView != ViewType.PREDICTIONS) {
                showView(ViewType.PREDICTIONS)
            }
        }
    }

    // AGGIORNATO: Gestisce 3 viste invece di 2
    private fun showView(viewType: ViewType) {
        currentView = viewType

        // Nascondi tutte le viste
        binding.mapView.visibility = View.GONE
        binding.statsContainer.visibility = View.GONE
        binding.predictionsContainer.visibility = View.GONE

        // Mostra la vista selezionata
        when (viewType) {
            ViewType.MAP -> {
                binding.mapView.visibility = View.VISIBLE
                updateButtonStyles(0)
            }
            ViewType.STATS -> {
                binding.statsContainer.visibility = View.VISIBLE
                updateButtonStyles(1)
            }
            ViewType.PREDICTIONS -> {
                binding.predictionsContainer.visibility = View.VISIBLE
                updateButtonStyles(2)
            }
        }
    }

    // AGGIORNATO: Gestisce 3 pulsanti invece di 2
    private fun updateButtonStyles(selectedIndex: Int) {
        val buttons = listOf(binding.btnMap, binding.btnStats, binding.btnPredictions)

        buttons.forEachIndexed { index, button ->
            if (index == selectedIndex) {
                button.setBackgroundResource(R.drawable.toggle_button_selected)
                button.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
            } else {
                button.setBackgroundResource(R.drawable.toggle_button_unselected)
                button.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
            }
        }
    }

    private fun setupMapView() {
        binding.mapView.getMapAsync { map ->
            googleMap = map

            // Configura la mappa
            map.uiSettings.apply {
                isZoomControlsEnabled = true
                isCompassEnabled = true
                isMyLocationButtonEnabled = false
            }

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
        predictionViewModel.loadPredictions()
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
        // Osserva StateFlow dal ViewModel per statistiche esistenti
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.completedTrips.collect { trips ->
                // Salva i dati nella cache
                cachedTrips = trips

                updateHeatmap(trips)
                updateMonthlyChart(trips)
                updateStatisticsCards(trips)
            }
        }

        // AGGIUNTO: Osserva le predizioni
        viewLifecycleOwner.lifecycleScope.launch {
            predictionViewModel.uiState.collect { uiState ->
                updatePredictionCards(uiState.prediction)

                if (uiState.error != null) {
                    Snackbar.make(binding.root, uiState.error, Snackbar.LENGTH_LONG)
                        .setAction("Riprova") {
                            predictionViewModel.loadPredictions()
                        }.show()
                    predictionViewModel.dismissError()
                }

                // Mostra/nascondi loading per predictions
                binding.predictionsProgressBar?.visibility = if (uiState.isLoading) View.VISIBLE else View.GONE
            }
        }
    }

    // AGGIUNTO: Metodo per aggiornare le cards delle predizioni
    @SuppressLint("SetTextI18n")
    private fun updatePredictionCards(prediction: com.example.travel_companion.domain.model.TravelPrediction?) {
        prediction?.let { pred ->
            binding.apply {
                // Previsione viaggi
                predictedTripsText?.text = pred.predictedTripsCount.toString()
                predictedTripsSubtext?.text = "viaggi previsti"

                // Previsione distanza
                if (pred.predictedDistance < 1.0) {
                    predictedDistanceText?.text = "0"
                    predictedDistanceSubtext?.text = "km previsti"
                } else {
                    predictedDistanceText?.text = String.format("%.0f", pred.predictedDistance)
                    predictedDistanceSubtext?.text = "km previsti"
                }

                // Indicatore di confidenza
                val confidencePercent = (pred.confidence * 100).toInt()
                confidenceText?.text = "$confidencePercent%"
                confidenceSubtext?.text = "affidabilità"

                // Colore basato sulla confidenza
                val confidenceColor = when {
                    pred.confidence > 0.7f -> ContextCompat.getColor(requireContext(), R.color.success_color)
                    pred.confidence > 0.4f -> ContextCompat.getColor(requireContext(), R.color.warning_color)
                    else -> ContextCompat.getColor(requireContext(), R.color.error_color)
                }
                confidenceText?.setTextColor(confidenceColor)

                // Trend indicator
                val trendText = when (pred.trend) {
                    com.example.travel_companion.domain.model.TravelTrend.INCREASING -> "In crescita"
                    com.example.travel_companion.domain.model.TravelTrend.DECREASING -> "In calo"
                    com.example.travel_companion.domain.model.TravelTrend.STABLE -> "Stabile"
                    com.example.travel_companion.domain.model.TravelTrend.INSUFFICIENT_DATA -> "Dati insufficienti"
                }
                trendIndicatorText?.text = trendText
            }
        }
    }

    // Mantieni tutti i metodi esistenti per heatmap, chart, etc.
    private fun updateHeatmap(trips: List<TripEntity>) {
        googleMap?.let { map ->
            // Rimuovi tutti i marker esistenti e la heatmap
            map.clear()
            heatmapTileOverlay?.remove()
            heatmapTileOverlay = null

            if (trips.isNotEmpty()) {
                // Usa coroutine per recuperare tutte le coordinate in background
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        // Recupera tutte le coordinate per i viaggi completati
                        val allCoordinates = viewModel.getAllCoordinatesForCompletedTrips()

                        if (allCoordinates.isNotEmpty()) {
                            // Campiona le coordinate se ce ne sono troppe per migliorare le performance
                            val sampledCoordinates = viewModel.sampleCoordinates(allCoordinates, 3)

                            // Converti le coordinate in LatLng per la heatmap
                            val heatmapData = sampledCoordinates.map { coordinate ->
                                LatLng(coordinate.latitude, coordinate.longitude)
                            }

                            // Crea il provider della heatmap con tutte le coordinate
                            val heatmapProvider = HeatmapTileProvider.Builder()
                                .data(heatmapData)
                                .radius(if (heatmapData.size > 500) 25 else 35) // Radius dinamico
                                .opacity(0.8)
                                .gradient(createCustomGradient()) // Gradiente personalizzato
                                .build()

                            // Aggiungi la heatmap alla mappa
                            heatmapTileOverlay = map.addTileOverlay(
                                TileOverlayOptions().tileProvider(heatmapProvider)
                            )

                            // Aggiungi marker solo per le destinazioni dei viaggi (opzionale)
                            trips.forEach { trip ->
                                val position = LatLng(trip.destinationLatitude, trip.destinationLongitude)
                                map.addMarker(
                                    MarkerOptions()
                                        .position(position)
                                        .title(trip.destination)
                                        .snippet("${trip.type} - ${formatDate(trip.startDate)}")
                                )
                            }

                            // Centra la mappa su tutte le coordinate
                            centerMapOnCoordinates(map, heatmapData)
                        }
                    } catch (e: Exception) {
                        Timber.tag(TAG).e(e, "Errore nel recupero coordinate: ${e.message}")
                    }
                }
            }
        }
    }

    // Metodo helper per centrare la mappa
    private fun centerMapOnCoordinates(map: GoogleMap, coordinates: List<LatLng>) {
        if (coordinates.isNotEmpty()) {
            val bounds = com.google.android.gms.maps.model.LatLngBounds.builder()
            coordinates.forEach { bounds.include(it) }

            try {
                // Usa un padding maggiore per mostrare meglio la heatmap
                map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), 150))
            } catch (e: Exception) {
                Timber.tag(TAG).w("Errore nel centrare la mappa con bounds: ${e.message}")
                // Se ci sono problemi con i bounds, centra sul punto centrale
                val centerLat = coordinates.map { it.latitude }.average()
                val centerLng = coordinates.map { it.longitude }.average()
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(centerLat, centerLng), 12f))
            }
        }
    }

    // Crea un gradiente personalizzato per la heatmap
    private fun createCustomGradient(): Gradient {
        val colors = intArrayOf(
            Color.rgb(102, 225, 0), // Verde
            Color.rgb(255, 255, 0), // Giallo
            Color.rgb(255, 165, 0), // Arancione
            Color.rgb(255, 0, 0)    // Rosso
        )
        val startPoints = floatArrayOf(0.1f, 0.4f, 0.7f, 1.0f)

        return Gradient(colors, startPoints)
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
                // Formatter per mostrare solo valori interi sull'asse Y
                valueFormatter = object : ValueFormatter() {
                    override fun getAxisLabel(value: Float, axis: com.github.mikephil.charting.components.AxisBase?): String {
                        return if (value > 0f) {
                            value.toInt().toString()
                        } else {
                            ""
                        }
                    }
                }
                granularity = 1f // Forza intervalli di 1
            }

            axisRight.isEnabled = false
            legend.isEnabled = false
        }
    }

    private fun updateMonthlyChart(trips: List<TripEntity>) {
        val monthlyData = calculateMonthlyTrips(trips)
        val entries = monthlyData.mapIndexed { index, count ->
            BarEntry(index.toFloat(), count.toFloat())
        }

        val dataSet = BarDataSet(entries, "Viaggi per Mese").apply {
            color = Color.parseColor("#2196F3")
            valueTextColor = Color.BLACK
            valueTextSize = 12f
            // Formatter personalizzato per mostrare solo valori > 0 sulle barre
            valueFormatter = object : ValueFormatter() {
                override fun getBarLabel(barEntry: BarEntry?): String {
                    return if (barEntry != null && barEntry.y > 0f) {
                        barEntry.y.toInt().toString()
                    } else {
                        ""
                    }
                }
            }
        }

        val barData = BarData(dataSet)
        barData.barWidth = 0.8f

        binding.monthlyChart.apply {
            data = barData
            // Formatter personalizzato per l'asse X per mostrare solo le etichette dei mesi con dati
            xAxis.valueFormatter = object : IndexAxisValueFormatter(getMonthLabels()) {
                override fun getAxisLabel(value: Float, axis: com.github.mikephil.charting.components.AxisBase?): String {
                    val index = value.toInt()
                    return if (index >= 0 && index < monthlyData.size && monthlyData[index] > 0) {
                        getMonthLabels()[index]
                    } else {
                        ""
                    }
                }
            }
            animateY(1000)
            invalidate()
        }
    }

    @SuppressLint("SetTextI18n", "DefaultLocale")
    private fun updateStatisticsCards(trips: List<TripEntity>) {
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

    private fun calculateMonthlyTrips(trips: List<TripEntity>): IntArray {
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
        outState.putInt("current_view", currentView.ordinal)
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