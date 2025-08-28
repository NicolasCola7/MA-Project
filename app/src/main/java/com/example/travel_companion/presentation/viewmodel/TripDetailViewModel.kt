package com.example.travel_companion.presentation.viewmodel

import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.lifecycle.*
import com.example.travel_companion.data.local.entity.CoordinateEntity
import com.example.travel_companion.data.local.entity.POIEntity
import com.example.travel_companion.data.local.entity.TripEntity
import com.example.travel_companion.data.repository.CoordinateRepository
import com.example.travel_companion.data.repository.POIRepository
import com.example.travel_companion.data.repository.TripRepository
import com.example.travel_companion.domain.model.TripStatus
import com.example.travel_companion.util.trip.TripScheduler
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TripDetailViewModel @Inject constructor(
    private val tripRepository: TripRepository,
    private val coordinateRepository: CoordinateRepository,
    private val poiRepository: POIRepository,
    private val tripScheduler: TripScheduler
) : ViewModel() {

    private var currentTripId: Long? = null

    // LiveData holding the current trip details
    private val _trip = MediatorLiveData<TripEntity?>()
    val trip: LiveData<TripEntity?> get() = _trip

    // LiveData holding coordinates of the trip
    private val _coordinates = MutableLiveData<List<CoordinateEntity>>()
    val coordinates: LiveData<List<CoordinateEntity>> get() = _coordinates

    // LiveData holding points of interest (POIs) of the trip
    private val _pois = MutableLiveData<List<POIEntity>>()
    val pois: LiveData<List<POIEntity>> get() = _pois

    /**
     * Loads trip details by ID
     * Removes previous observation if it exists
     */
    fun loadTrip(tripId: Long) {
        currentTripId?.let { oldId ->
            _trip.removeSource(tripRepository.getTripById(oldId))
        }

        currentTripId = tripId

        val tripLiveData = tripRepository.getTripById(tripId)
        _trip.addSource(tripLiveData) { tripEntity ->
            _trip.value = tripEntity
        }
    }

    /**
     * Inserts a new coordinate for a trip
     */
    fun insertCoordinate(lat: Double, long: Double, tripId: Long) {
        val newCoordinate = CoordinateEntity(
            latitude = lat,
            longitude = long,
            tripId = tripId
        )

        viewModelScope.launch(Dispatchers.IO) {
            coordinateRepository.insertCoordinate(newCoordinate)
        }
    }

    /**
     * Loads all coordinates associated with a trip
     */
    fun loadCoordinates(tripId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val loadedTripCoordinates = coordinateRepository.getCoordinatesForTrip(tripId)
            _coordinates.postValue(loadedTripCoordinates)
        }
    }

    /**
     * Loads all POIs associated with a trip
     */
    fun loadPOIs(tripId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val loadedPOIs = poiRepository.getPOIs(tripId)
            _pois.postValue(loadedPOIs)
        }
    }

    /**
     * Inserts a new POI for a trip
     */
    fun insertPOI(tripId: Long, pos: LatLng, name: String, placeId: String) {
        val newPOI = POIEntity(
            tripId = tripId,
            latitude = pos.latitude,
            longitude = pos.longitude,
            name = name,
            placeId = placeId
        )

        viewModelScope.launch(Dispatchers.IO) {
            poiRepository.insertPOI(newPOI)
        }
    }

    /**
     * Deletes a POI by name for a specific trip
     */
    fun deletePOI(poiName: String, tripId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            poiRepository.deletePOI(poiName, tripId)
        }
    }

    /**
     * Marks the trip as finished and cancels any scheduled alarms
     */
    fun finishTrip() {
        val updated = _trip.value?.copy(
            status = TripStatus.FINISHED,
            endDate = System.currentTimeMillis()
        ) ?: return

        viewModelScope.launch(Dispatchers.IO) {
            tripRepository.updateTrip(updated)
        }

        _trip.value?.id?.let { tripId ->
            tripScheduler.cancelTripAlarms(mutableListOf(tripId))
        }
    }

    /**
     * Updates the tracked time and distance of the trip
     */
    fun updateTimeAndDistanceTracked(time: Long, distance: Double) {
        val updated = _trip.value?.copy(
            timeTracked = time,
            trackedDistance = distance
        ) ?: return

        viewModelScope.launch(Dispatchers.IO) {
            tripRepository.updateTrip(updated)
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Clean up any remaining sources when ViewModel is destroyed
        currentTripId?.let { tripId ->
            _trip.removeSource(tripRepository.getTripById(tripId))
        }
    }
}
