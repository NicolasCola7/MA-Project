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
    val endDate: Long,
    val type: String,
    val status: TripStatus = TripStatus.PLANNED,
    val imageData: ByteArray? = null,
    val trackedDistance: Double = 0.0,
    val destinationLatitude: Double,
    val destinationLongitude: Double,
    val timeTracked: Long = 0
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TripEntity

        if (id != other.id) return false
        if (destination != other.destination) return false
        if (startDate != other.startDate) return false
        if (endDate != other.endDate) return false
        if (type != other.type) return false
        if (status != other.status) return false
        if (imageData != null) {
            if (other.imageData == null) return false
            if (!imageData.contentEquals(other.imageData)) return false
        } else if (other.imageData != null) return false
        if (trackedDistance != other.trackedDistance) return false
        if (destinationLatitude != other.destinationLatitude) return false
        if (destinationLongitude != other.destinationLongitude) return false
        if (timeTracked != other.timeTracked) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + destination.hashCode()
        result = 31 * result + startDate.hashCode()
        result = 31 * result + endDate.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + status.hashCode()
        result = 31 * result + (imageData?.contentHashCode() ?: 0)
        result = 31 * result + trackedDistance.hashCode()
        result = 31 * result + destinationLatitude.hashCode()
        result = 31 * result + destinationLongitude.hashCode()
        result = 31 * result + timeTracked.hashCode()
        return result
    }


}