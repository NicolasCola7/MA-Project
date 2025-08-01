package com.example.travel_companion.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travel_companion.data.local.entity.TripEntity
import com.example.travel_companion.data.repository.TripRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val tripRepository: TripRepository
) : ViewModel() {

    private val _todayTrip = MutableLiveData<TripEntity?>()
    val todayTrip: LiveData<TripEntity?> get() = _todayTrip

    private val _currentDate = MutableLiveData<String>()
    val currentDate: LiveData<String> get() = _currentDate

    init {
        // Imposta la data odierna
        val dateFormat = SimpleDateFormat("EEEE d MMMM yyyy", Locale.getDefault())
        _currentDate.value = dateFormat.format(Date())

        // Carica eventuale viaggio di oggi
        loadTodayTrip()
    }

    private fun loadTodayTrip() {
        val now = System.currentTimeMillis()
        viewModelScope.launch(Dispatchers.IO) {
            val trip = tripRepository.getTripAtTime(now)
            _todayTrip.postValue(trip)
        }
    }
}
