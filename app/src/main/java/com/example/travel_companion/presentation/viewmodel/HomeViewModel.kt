package com.example.travel_companion.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travel_companion.R
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

    val currentDate: LiveData<String> = MutableLiveData<String>().apply {
        val dateFormat = SimpleDateFormat("EEEE d MMMM yyyy", Locale.getDefault())
        value = dateFormat.format(Date())
    }

    //si aggiorna automaticamente se il viaggio cambia o viene eliminato
    val todayTrip: LiveData<TripEntity?> =
        tripRepository.getTripAtTimeLive(System.currentTimeMillis())
}
