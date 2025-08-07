package com.example.travel_companion.presentation.viewmodel

import androidx.lifecycle.*
import com.example.travel_companion.data.local.entity.NoteEntity
import com.example.travel_companion.data.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotesViewModel @Inject constructor (
    private val noteRepository: NoteRepository
) : ViewModel() {

    //I dati sono esposti tramite LiveData, così il fragment può osservarli.
    private val _notes = MutableLiveData<List<NoteEntity>>()
    val notes: LiveData<List<NoteEntity>> get() = _notes

    //Le note vengono caricate in background (Dispatchers.IO) e postate nel LiveData.
    fun loadNotes(tripId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val noteList = noteRepository.getNotesByTripId(tripId)
            _notes.postValue(noteList)
        }
    }

    fun insertNote(tripId: Long, title: String, content: String) {
        val newNote = NoteEntity(
            title = title,
            content = content,
            tripId = tripId
        )

        viewModelScope.launch(Dispatchers.IO) {
            noteRepository.insert(newNote)
        }
    }

    fun deleteNotes(tripIds: List<Long>) {
        viewModelScope.launch {
            noteRepository.deleteNotes(tripIds)
        }
    }
}