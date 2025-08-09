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

    private val _currentTripId = MutableLiveData<Long>()

    val notes: LiveData<List<NoteEntity>> = _currentTripId.switchMap { tripId ->
        noteRepository.getNotesByTripId(tripId)
    }

    fun loadNotes(tripId: Long) {
        _currentTripId.value = tripId
    }

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
                Timber.e("Errore nell'inserimento delle note")
            }
        }
    }

    fun getNoteById(id: Long): LiveData<NoteEntity> {
        return noteRepository.getNoteById(id)
    }

    fun updateNote(note: NoteEntity) {
        viewModelScope.launch {
            noteRepository.updateNote(note)
        }
    }

    fun deleteNotes(noteIds: List<Long>) {
        viewModelScope.launch {
            try {
                noteRepository.deleteNotes(noteIds)
            } catch (e: Exception) {
                Timber.e("Errore nella cancellazione delle note")
            }
        }
    }
}