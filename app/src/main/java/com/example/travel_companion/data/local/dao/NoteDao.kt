package com.example.travel_companion.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.travel_companion.data.local.entity.NoteEntity

@Dao
interface NoteDao {
    @Insert
    suspend fun insert(note: NoteEntity)

    @Query("SELECT * FROM note WHERE tripId = :tripId ORDER BY timestamp DESC")
    suspend fun getNotesByTripId(tripId: Long): List<NoteEntity>
}