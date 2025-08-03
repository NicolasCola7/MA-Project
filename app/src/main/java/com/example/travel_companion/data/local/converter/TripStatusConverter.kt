package com.example.travel_companion.data.local.converter

import androidx.room.TypeConverter
import com.example.travel_companion.domain.model.TripStatus

class TripStatusConverter {
    @TypeConverter
    fun fromTripStatus(status: TripStatus): String {
        return status.name
    }

    @TypeConverter
    fun toTripStatus(status: String): TripStatus {
        return TripStatus.valueOf(status)
    }
}