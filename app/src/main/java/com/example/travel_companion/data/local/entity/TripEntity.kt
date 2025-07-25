package com.example.travel_companion.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.travel_companion.domain.model.TripStatus

@Entity(tableName = "trip")
data class TripEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val destination: String,
    val startDate: Long,
    val endDate: Long?,
    val type: String,
    val status: TripStatus = TripStatus.PLANNED
)
