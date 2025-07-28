package com.example.travel_companion.presentation.viewmodel

import com.example.travel_companion.domain.model.Trip
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.lifecycle.*
import com.example.travel_companion.data.local.entity.CoordinateEntity
import com.example.travel_companion.data.local.entity.PhotoEntity
import com.example.travel_companion.data.local.entity.TripEntity
import com.example.travel_companion.data.repository.CoordinateRepository
import com.example.travel_companion.data.repository.PhotoRepository
import com.example.travel_companion.data.repository.TripRepository
import com.example.travel_companion.domain.model.TripStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TripDetailViewModel  @Inject constructor (
    private val tripRepository: TripRepository,
    private val coordinateRepository: CoordinateRepository,
) : ViewModel() {

    private val _trip = MutableLiveData<TripEntity?>()
    val trip: LiveData<TripEntity?> get() = _trip

    private val _coordinates = MutableLiveData<List<CoordinateEntity?>>()
    val coordinates: LiveData<List<CoordinateEntity?>> get() = _coordinates

    fun loadTrip(tripId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val loadedTrip = tripRepository.getTripById(tripId)
            _trip.postValue(loadedTrip)

            val loadedTripCoordinates = coordinateRepository.getCoordinatesForTrip(tripId)
            _coordinates.postValue(loadedTripCoordinates)
        }
    }

    fun insertCoordinate(lat: Double, long: Double, tripId: Long) {
        val newCoordinate = CoordinateEntity (
            latitude = lat,
            longitude = long,
            tripId = tripId
        )

        viewModelScope.launch(Dispatchers.IO) {
            coordinateRepository.insertCoordinate(newCoordinate)
        }
    }

    fun deleteTrip() {
        viewModelScope.launch(Dispatchers.IO) {
            tripRepository.deleteTrip(trip.value!!)
        }
    }


    fun updateTripStatus(status: TripStatus) {
        val updated = _trip.value?.copy(status = status) ?: return
        viewModelScope.launch(Dispatchers.IO) {
            tripRepository.updateTrip(updated)
            _trip.postValue(updated)
        }
    }

}