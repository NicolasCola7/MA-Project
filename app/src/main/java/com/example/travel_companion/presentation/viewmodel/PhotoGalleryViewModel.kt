package com.example.travel_companion.presentation.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travel_companion.data.local.entity.PhotoEntity
import com.example.travel_companion.data.repository.PhotoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.FileNotFoundException
import javax.inject.Inject

@HiltViewModel
class PhotoGalleryViewModel @Inject constructor (
    private var photoRepository: PhotoRepository
): ViewModel()  {

    private val _photos = MutableLiveData<List<PhotoEntity>>()
    val photos: LiveData<List<PhotoEntity>> get() = _photos

    fun loadPhotos(tripId: Long): LiveData<List<PhotoEntity>> {
        return photoRepository.getPhotosByTripId(tripId)
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

    fun deletePhotos(photoIds: List<Long>) {
        viewModelScope.launch(Dispatchers.IO) {
            photoRepository.deletePhotos(photoIds)
        }
    }

    // Aggiungi questo metodo al tuo PhotoGalleryViewModel

    /**
     * Sincronizza il database con le foto realmente disponibili nel sistema
     */
    fun syncPhotosWithSystem(context: Context, tripId: Long) {
        // Usa il LiveData esistente
        val currentPhotos = photos.value ?: emptyList()

        viewModelScope.launch {
            val orphanedPhotoIds = mutableListOf<Long>()

            withContext(Dispatchers.IO) {
                currentPhotos.forEach { photo ->
                    if (!isPhotoAccessible(context, photo.uri)) {
                        orphanedPhotoIds.add(photo.id)
                    }
                }
            }

            if (orphanedPhotoIds.isNotEmpty()) {
                photoRepository.deletePhotos(orphanedPhotoIds)
                Timber.d("Removed ${orphanedPhotoIds.size} orphaned photo references")
            }
        }
    }

    private fun isPhotoAccessible(context: Context, uriString: String): Boolean {
        return try {
            val uri = Uri.parse(uriString)
            context.contentResolver.openInputStream(uri)?.use { true } ?: false
        } catch (e: Exception) {
            when (e) {
                is SecurityException,
                is FileNotFoundException,
                is IllegalArgumentException -> false
                else -> true // In caso di errore sconosciuto, assumiamo che sia accessibile
            }
        }
    }
}