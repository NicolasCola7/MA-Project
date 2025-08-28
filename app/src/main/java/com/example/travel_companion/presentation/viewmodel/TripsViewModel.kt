package com.example.travel_companion.presentation.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travel_companion.data.local.database.Converters
import com.example.travel_companion.data.local.entity.TripEntity
import com.example.travel_companion.data.repository.TripRepository
import com.example.travel_companion.util.Utils
import com.example.travel_companion.util.trip.TripScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*
import javax.inject.Inject

@HiltViewModel
class TripsViewModel @Inject constructor(
    private val tripRepository: TripRepository,
    private val tripScheduler: TripScheduler
) : ViewModel() {

    val trips: LiveData<List<TripEntity>> = tripRepository.getAllTrips()
    var selectedDestinationName: String = ""
    var selectedPlaceImageData: ByteArray? = null
    private val converters = Converters()

    // UI events
    private val _uiEvent = MutableLiveData<Event>()
    val uiEvent: LiveData<Event> get() = _uiEvent

    /**
     * Handles the creation of a new trip
     */
    fun onCreateTripClicked(
        destination: String,
        startDateStr: String,
        endDateStr: String,
        type: String,
        lat: Double,
        long: Double
    ) {
        // Basic validation
        if (destination.isBlank() || startDateStr.isBlank() ||
            (type == "Viaggio di più giorni" && endDateStr == "Seleziona data e ora")
        ) {
            _uiEvent.value = Event.ShowMessage("Please fill in all required fields")
            return
        }

        val startDate = Utils.dateTimeFormat.parse(startDateStr)
        val endDate = if (type == "Viaggio di più giorni") Utils.dateTimeFormat.parse(endDateStr) else null

        // Validate date formats
        if (startDate == null || (type == "Viaggio di più giorni" && endDate == null)) {
            _uiEvent.value = Event.ShowMessage("Invalid date format")
            return
        }

        val now = System.currentTimeMillis()
        if (startDate.time <= now) {
            _uiEvent.value = Event.ShowMessage("Start date must be in the future")
            return
        }

        if (type == "Viaggio di più giorni") {
            val startCal = Calendar.getInstance().apply { time = startDate }
            val endCal = Calendar.getInstance().apply { time = endDate!! }

            val sameDay = startCal.get(Calendar.YEAR) == endCal.get(Calendar.YEAR) &&
                    startCal.get(Calendar.DAY_OF_YEAR) == endCal.get(Calendar.DAY_OF_YEAR)

            if (sameDay) {
                _uiEvent.value = Event.ShowMessage("End date must be on a later day")
                return
            }

            if (endDate!!.time <= startDate.time) {
                _uiEvent.value = Event.ShowMessage("End date must be after start date")
                return
            }
        }

        val calendar = Calendar.getInstance().apply { time = startDate }

        // Determine the final end time for different trip types
        val finalEndDate: Long = when (type) {
            "Viaggio di più giorni" -> endDate!!.time
            "Gita Giornaliera", "Viaggio Locale" -> {
                if (endDateStr.isBlank()) {
                    // Set end of day if no specific time is provided
                    calendar.set(Calendar.HOUR_OF_DAY, 23)
                    calendar.set(Calendar.MINUTE, 59)
                    calendar.set(Calendar.SECOND, 59)
                    calendar.timeInMillis
                } else {
                    Timber.d(endDateStr)
                    val timeParts = endDateStr.split(":")
                    if (timeParts.size != 2) {
                        _uiEvent.value = Event.ShowMessage("Enter end time in HH:mm format")
                        return
                    }
                    val hour = timeParts[0].toIntOrNull()
                    val minute = timeParts[1].toIntOrNull()
                    if (hour == null || minute == null) {
                        _uiEvent.value = Event.ShowMessage("Invalid time")
                        return
                    }
                    calendar.set(Calendar.HOUR_OF_DAY, hour)
                    calendar.set(Calendar.MINUTE, minute)
                    calendar.set(Calendar.SECOND, 0)

                    val endMillis = calendar.timeInMillis
                    if (endMillis <= startDate.time) {
                        _uiEvent.value = Event.ShowMessage("End time must be after start time")
                        return
                    }
                    endMillis
                }
            }
            else -> {
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.timeInMillis
            }
        }

        insertTrip(destination, startDate.time, finalEndDate, type, lat, long)
    }

    /**
     * Inserts a new trip into the database, checking for conflicts
     */
    private fun insertTrip(destination: String, start: Long, end: Long, type: String, lat: Double, long: Double) {
        val newTrip = TripEntity(
            destination = destination,
            startDate = start,
            endDate = end,
            type = type,
            imageData = selectedPlaceImageData,
            destinationLatitude = lat,
            destinationLongitude = long
        )

        // Attempt to insert trip if no conflicts exist
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val hasConflict = tripRepository.isTripOverlapping(start, end)
                if (!hasConflict) {
                    val id = tripRepository.addTrip(newTrip)
                    tripScheduler.scheduleTrip(id, newTrip.startDate, newTrip.endDate)
                    _uiEvent.postValue(Event.Success)
                } else {
                    _uiEvent.postValue(Event.ShowMessage("A trip already exists in this time range"))
                }
            } catch (e: Exception) {
                _uiEvent.postValue(Event.ShowMessage("Error creating the trip"))
                Timber.d(e.stackTraceToString())
            }
        }
    }

    /**
     * Deletes trips and cancels their alarms
     */
    fun deleteTrips(tripIds: List<Long>) {
        viewModelScope.launch {
            tripRepository.deleteTrips(tripIds)
            tripScheduler.cancelTripAlarms(tripIds)
        }
    }

    /**
     * Saves the selected place image
     */
    fun setPlaceImage(bitmap: Bitmap) {
        // Resize the image to optimize database space
        val resizedBitmap = Utils.resizeBitmap(bitmap, 400, 300)
        // Convert bitmap to ByteArray for storage
        selectedPlaceImageData = converters.fromBitmap(resizedBitmap)
    }

    /**
     * Resets selected data when leaving the fragment
     */
    fun resetData() {
        selectedDestinationName = ""
        selectedPlaceImageData = null
    }

    // UI event types
    sealed class Event {
        data class ShowMessage(val message: String) : Event()
        object Success : Event()
    }
}
