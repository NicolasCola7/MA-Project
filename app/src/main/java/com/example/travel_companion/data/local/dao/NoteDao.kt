package com.example.travel_companion.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.travel_companion.data.local.entity.NoteEntity

@Dao
interface NoteDao {
    @Insert
    suspend fun insert(note: NoteEntity)

    @Query("SELECT * FROM note WHERE tripId = :tripId ORDER BY timestamp DESC")
    fun getNotesByTripId(tripId: Long): LiveData<List<NoteEntity>>

    @Query("DELETE FROM note WHERE id IN (:noteIds)")
    suspend fun deleteNotes(noteIds: List<Long>)
}