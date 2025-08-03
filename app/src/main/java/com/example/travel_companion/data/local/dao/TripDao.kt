package com.example.travel_companion.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.travel_companion.data.local.entity.TripEntity
import com.example.travel_companion.domain.model.TripStatus

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

    @Query("SELECT * FROM trip WHERE (:newStart BETWEEN startDate AND endDate) OR (:newEnd BETWEEN startDate AND endDate) OR (startDate BETWEEN :newStart AND :newEnd)")
    suspend fun getOverlappingTrips(newStart: Long, newEnd: Long): List<TripEntity>

    @Query("SELECT * FROM trip WHERE :timestamp BETWEEN startDate AND endDate LIMIT 1")
    fun getTripAtTimeLive(timestamp: Long): LiveData<TripEntity?>

    @Query("SELECT * FROM trip WHERE startDate > :timestamp ORDER BY startDate ASC LIMIT 1")
    fun getNextPlannedTripLive(timestamp: Long): LiveData<TripEntity?>

    // Nuovi metodi per gestire gli stati
    @Query("UPDATE trip SET status = :status WHERE id = :tripId")
    suspend fun updateTripStatus(tripId: Long, status: TripStatus)

    @Query("SELECT * FROM trip WHERE status = :status ORDER BY startDate ASC")
    fun getTripsByStatus(status: TripStatus): LiveData<List<TripEntity>>

    @Query("SELECT * FROM trip WHERE status IN (:statuses) ORDER BY startDate ASC")
    fun getTripsByStatuses(statuses: List<TripStatus>): LiveData<List<TripEntity>>

    // Query per ottenere viaggi che dovrebbero cambiare stato
    @Query("SELECT * FROM trip WHERE status = 'PLANNED' AND startDate <= :currentTime")
    suspend fun getTripsToStart(currentTime: Long): List<TripEntity>

    @Query("SELECT * FROM trip WHERE status = 'STARTED' AND endDate <= :currentTime")
    suspend fun getTripsToFinish(currentTime: Long): List<TripEntity>

    // Batch update per efficienza
    @Query("UPDATE trip SET status = 'STARTED' WHERE status = 'PLANNED' AND startDate <= :currentTime")
    suspend fun updatePlannedTripsToStarted(currentTime: Long): Int

    @Query("UPDATE trip SET status = 'FINISHED' WHERE status = 'STARTED' AND endDate <= :currentTime")
    suspend fun updateStartedTripsToFinished(currentTime: Long): Int
}