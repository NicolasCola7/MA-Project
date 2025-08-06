package com.example.travel_companion.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travel_companion.data.local.entity.NoteEntity
import com.example.travel_companion.data.local.entity.PhotoEntity
import com.example.travel_companion.data.repository.PhotoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PhotoGalleryViewModel @Inject constructor (
    private var photoRepository: PhotoRepository
): ViewModel()  {

    private val _photos = MutableLiveData<List<PhotoEntity>>()
    val photos: LiveData<List<PhotoEntity>> get() = _photos

    fun loadPhotos(tripId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val noteList = photoRepository.getPhotosByTripId(tripId)
            _photos.postValue(noteList)
        }
    }

    fun insert(tripId: Long, uri: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val newPhoto = PhotoEntity (
                tripId = tripId,
                uri = uri
            )

            photoRepository.insert(newPhoto)
        }
    }
}