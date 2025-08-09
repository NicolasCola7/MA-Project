package com.example.travel_companion.data.repository

import androidx.lifecycle.LiveData
import com.example.travel_companion.data.local.dao.NoteDao
import com.example.travel_companion.data.local.entity.NoteEntity
import javax.inject.Inject

class NoteRepository @Inject constructor (
    private val noteDao: NoteDao
) {
    suspend fun insert(note: NoteEntity) {
        noteDao.insert(note);
    }

    fun getNotesByTripId(tripId: Long): LiveData<List<NoteEntity>> {
        return noteDao.getNotesByTripId(tripId)
    }

    suspend fun deleteNotes(noteIds: List<Long>) {
        noteDao.deleteNotes(noteIds)
    }

    suspend fun updateNote(note: NoteEntity){
        noteDao.updateNote(note)
    }

    fun getNoteById(id: Long): LiveData<NoteEntity> {
        return noteDao.getNoteById(id)
    }
}