package com.example.travel_companion.data.local.dao


import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.travel_companion.data.local.entity.CoordinateEntity

@Dao
interface CoordinateDao {
    @Query("SELECT * FROM coordinates WHERE tripId = :tripId ORDER BY timestamp ASC")
    fun getCoordinatesForTrip(tripId: Long): List<CoordinateEntity>

    @Insert
    suspend fun insertCoordinate(coordinate: CoordinateEntity)

}