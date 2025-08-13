package com.example.travel_companion.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.travel_companion.data.local.entity.POIEntity

@Dao
interface POIDao {
    @Query("SELECT * FROM poi WHERE tripId = :tripId")
    fun getPOIs(tripId: Long): List<POIEntity>

    @Insert
    suspend fun insertPOI(coordinate: POIEntity)

    @Query("DELETE FROM poi WHERE name = :poiName AND tripId = :tripId")
    fun deletePOI(poiName: String, tripId: Long)
}