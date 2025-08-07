package com.example.travel_companion.data.repository

import androidx.lifecycle.LiveData
import com.example.travel_companion.data.local.dao.NoteDao
import com.example.travel_companion.data.local.entity.NoteEntity
import javax.inject.Inject

class NoteRepository @Inject constructor (
    private val noteDao: NoteDao
) {
    suspend fun insert(photo: NoteEntity) {
        noteDao.insert(photo);
    }

    fun getNotesByTripId(tripId: Long): LiveData<List<NoteEntity>> {
        return noteDao.getNotesByTripId(tripId)
    }

    suspend fun deleteNotes(tripIds: List<Long>) {
        noteDao.deleteNotes(tripIds)
    }

    fun getNoteById(id: Long): LiveData<NoteEntity> {
        return noteDao.getNoteById(id)
    }
}