package com.example.travel_companion.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.travel_companion.data.local.entity.TripEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {
    @Insert
    suspend fun insert(trip: TripEntity): Long

    @Query("SELECT * FROM trip ORDER BY startDate DESC")
    fun getAll(): LiveData<List<TripEntity>>

    @Query("SELECT * FROM trip WHERE id = :tripId LIMIT 1")
    suspend fun getTripById(tripId: Long): TripEntity?

    @Update
    suspend fun updateTrip(trip: TripEntity)

    @Delete
    suspend fun deleteTrip(trip: TripEntity)

    @Query(" SELECT * FROM trip WHERE (:newStart BETWEEN startDate AND endDate) OR (:newEnd BETWEEN startDate AND endDate) OR (startDate BETWEEN :newStart AND :newEnd)")
    suspend fun getOverlappingTrips(newStart: Long, newEnd: Long): List<TripEntity>

    @Query("SELECT * FROM trip WHERE startDate <= :timestamp AND endDate >= :timestamp LIMIT 1")
    fun getTripAtTimeFlow(timestamp: Long): Flow<TripEntity?>
}