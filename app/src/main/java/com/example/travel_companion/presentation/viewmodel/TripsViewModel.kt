package com.example.travel_companion.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travel_companion.data.local.entity.TripEntity
import com.example.travel_companion.data.repository.TripRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TripsViewModel  @Inject constructor(
    private val tripRepository: TripRepository
): ViewModel() {

    val trips: LiveData<List<TripEntity>> = tripRepository.getAllTrips()
    var selectedDestinationName: String = ""


    fun insertTrip(destination: String, start: Long, end: Long?, type: String) {
        val newTrip = TripEntity(
            destination = destination,
            startDate = start,
            endDate = end,
            type = type
        )

        viewModelScope.launch(Dispatchers.IO) {
            tripRepository.addTrip(newTrip)
        }
    }
}