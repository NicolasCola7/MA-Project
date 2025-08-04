package com.example.travel_companion.presentation.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travel_companion.data.local.database.Converters
import com.example.travel_companion.data.local.entity.TripEntity
import com.example.travel_companion.data.repository.TripRepository
import com.example.travel_companion.presentation.Utils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class TripsViewModel @Inject constructor(
    private val tripRepository: TripRepository
) : ViewModel() {

    val trips: LiveData<List<TripEntity>> = tripRepository.getAllTrips()
    var selectedDestinationName: String = ""
    var selectedPlaceImageData: ByteArray? = null
    private val converters = Converters()

    // Eventi UI
    private val _uiEvent = MutableLiveData<Event>()
    val uiEvent: LiveData<Event> get() = _uiEvent

    fun onCreateTripClicked(destination: String, startDateStr: String, endDateStr: String, type: String) {
        // Validazioni di base
        if (destination.isBlank() || startDateStr.isBlank() || (type == "Viaggio di più giorni" && endDateStr.isBlank())) {
            _uiEvent.value = Event.ShowMessage("Compila i campi obbligatori")
            return
        }

        val startDate = Utils.dateTimeFormat.parse(startDateStr)
        val endDate = if (type == "Viaggio di più giorni") Utils.dateTimeFormat.parse(endDateStr) else null

        //controllo su date di avvio
        if (startDate == null || (type == "Viaggio di più giorni" && endDate == null)) {
            _uiEvent.value = Event.ShowMessage("Formato data non valido")
            return
        }

        val now = System.currentTimeMillis()
        if (startDate.time <= now) {
            _uiEvent.value = Event.ShowMessage("La data di inizio deve essere futura")
            return
        }

        if (type == "Viaggio di più giorni") {
            val startCal = Calendar.getInstance().apply { time = startDate }
            val endCal = Calendar.getInstance().apply { time = endDate!! }

            val sameDay = startCal.get(Calendar.YEAR) == endCal.get(Calendar.YEAR) &&
                    startCal.get(Calendar.DAY_OF_YEAR) == endCal.get(Calendar.DAY_OF_YEAR)

            if (sameDay) {
                _uiEvent.value = Event.ShowMessage("La data di fine deve essere in un giorno successivo")
                return
            }

            if (endDate!!.time <= startDate.time) {
                _uiEvent.value = Event.ShowMessage("La data di fine deve essere dopo la data di inizio")
                return
            }
        }

        val calendar = Calendar.getInstance()
        calendar.time = startDate

        //controllo su data di fine
        val finalEndDate: Long = when (type) {
            "Viaggio di più giorni" -> endDate!!.time

            "Gita Giornaliera", "Viaggio Locale" -> {
                if (endDateStr.isBlank()) {
                    calendar.set(Calendar.HOUR_OF_DAY, 23)
                    calendar.set(Calendar.MINUTE, 59)
                    calendar.set(Calendar.SECOND, 59)
                    calendar.timeInMillis
                } else {
                    val timeParts = endDateStr.split(":")
                    if (timeParts.size != 2) {
                        _uiEvent.value = Event.ShowMessage("Inserisci l'ora di fine nel formato HH:mm")
                        return
                    }
                    val hour = timeParts[0].toIntOrNull()
                    val minute = timeParts[1].toIntOrNull()
                    if (hour == null || minute == null) {
                        _uiEvent.value = Event.ShowMessage("Orario non valido")
                        return
                    }
                    calendar.set(Calendar.HOUR_OF_DAY, hour)
                    calendar.set(Calendar.MINUTE, minute)
                    calendar.set(Calendar.SECOND, 0)

                    val endMillis = calendar.timeInMillis
                    if (endMillis <= startDate.time) {
                        _uiEvent.value = Event.ShowMessage("L'ora di fine deve essere successiva a quella di inizio")
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

        insertTrip(destination, startDate.time, finalEndDate, type)
    }

    private fun insertTrip(destination: String, start: Long, end: Long, type: String) {
        val newTrip = TripEntity(
            destination = destination,
            startDate = start,
            endDate = end,
            type = type,
            imageData = selectedPlaceImageData
        )

        //provo ad inserire un nuovo viaggio sul db, se il db mi dice che non ci sono conflitti lo inserisco
        viewModelScope.launch(Dispatchers.IO) {
            val hasConflict = tripRepository.isTripOverlapping(start, end)
            if (!hasConflict) {
                tripRepository.addTrip(newTrip)
                _uiEvent.postValue(Event.Success)
            } else {
                _uiEvent.postValue(Event.ShowMessage("Esiste già un viaggio in questo intervallo di tempo"))
            }
        }
    }

    // Funzione per salvare l'immagine del luogo selezionato
    fun setPlaceImage(bitmap: Bitmap) {
        // Ridimensiona l'immagine per ottimizzare lo spazio su database
        val resizedBitmap = Utils.resizeBitmap(bitmap, 400, 300)
        // Usa il converter per convertire bitmap a ByteArray
        selectedPlaceImageData = converters.fromBitmap(resizedBitmap)
    }

    // Funzione per ottenere il bitmap da ByteArray
    fun getTripImage(trip: TripEntity): Bitmap? {
        return trip.imageData?.let { converters.toBitmap(it) }
    }

    // Reset dei dati quando si esce dal fragment
    fun resetData() {
        selectedDestinationName = ""
        selectedPlaceImageData = null
    }

    sealed class Event {
        data class ShowMessage(val message: String) : Event()
        object Success : Event()
    }
}