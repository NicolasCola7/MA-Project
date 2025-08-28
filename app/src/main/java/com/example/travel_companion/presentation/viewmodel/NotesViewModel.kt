package com.example.travel_companion.presentation.viewmodel

import androidx.lifecycle.*
import com.example.travel_companion.data.local.entity.NoteEntity
import com.example.travel_companion.data.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class NotesViewModel @Inject constructor(
    private val noteRepository: NoteRepository
) : ViewModel() {

    // LiveData holding the current trip ID
    private val _currentTripId = MutableLiveData<Long>()

    // LiveData for notes of the currently selected trip
    val notes: LiveData<List<NoteEntity>> = _currentTripId.switchMap { tripId ->
        noteRepository.getNotesByTripId(tripId)
    }

    /**
     * Loads notes for a specific trip by setting the current trip ID.
     */
    fun loadNotes(tripId: Long) {
        _currentTripId.value = tripId
    }

    /**
     * Inserts a new note for a trip.
     */
    fun insertNote(tripId: Long, title: String, content: String) {
        viewModelScope.launch {
            try {
                val note = NoteEntity(
                    tripId = tripId,
                    title = title,
                    content = content,
                    timestamp = System.currentTimeMillis()
                )
                noteRepository.insert(note)
            } catch (e: Exception) {
                Timber.e("Error inserting note")
            }
        }
    }

    /**
     * Returns a LiveData observing a specific note by its ID.
     */
    fun getNoteById(id: Long): LiveData<NoteEntity> {
        return noteRepository.getNoteById(id)
    }

    /**
     * Updates an existing note.
     */
    fun updateNote(note: NoteEntity) {
        viewModelScope.launch {
            noteRepository.updateNote(note)
        }
    }

    /**
     * Deletes a list of notes by their IDs.
     */
    fun deleteNotes(noteIds: List<Long>) {
        viewModelScope.launch {
            try {
                noteRepository.deleteNotes(noteIds)
            } catch (e: Exception) {
                Timber.e("Error deleting notes")
            }
        }
    }
}
