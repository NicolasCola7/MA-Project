package com.example.travel_companion.data.repository

import com.example.travel_companion.data.local.dao.NoteDao
import com.example.travel_companion.data.local.entity.NoteEntity
import com.example.travel_companion.data.local.entity.PhotoEntity
import javax.inject.Inject

class NoteRepository @Inject constructor (
    private val noteDao: NoteDao
) {
    suspend fun insert(photo: NoteEntity) {
        noteDao.insert(photo);
    }

    suspend fun getNotesByTripId(tripId: Long): List<NoteEntity> {
        return noteDao.getNotesByTripId(tripId)
    }

    suspend fun deleteNotes(tripIds: List<Long>) {
        noteDao.deleteNotes(tripIds)
    }
}