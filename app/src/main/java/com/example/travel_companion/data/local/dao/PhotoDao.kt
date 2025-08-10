package com.example.travel_companion.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.travel_companion.data.local.entity.PhotoEntity

@Dao
interface PhotoDao {
    @Insert
    suspend fun insert(photo: PhotoEntity)

    @Query("SELECT * FROM trip_photos WHERE tripId = :tripId")
    fun getPhotosByTripId(tripId: Long): LiveData<List<PhotoEntity>>

    @Query("SELECT * FROM trip_photos WHERE id IN (:photoIds)")
    suspend fun getPhotosByIds(photoIds: List<Long>): List<PhotoEntity>

    @Query("DELETE FROM trip_photos WHERE id IN (:photoIds)")
    suspend fun deletePhotosByIds(photoIds: List<Long>)

}