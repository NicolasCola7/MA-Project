package com.example.travel_companion.presentation.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.switchMap
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
class PhotoGalleryViewModel @Inject constructor(
    private val photoRepository: PhotoRepository
) : ViewModel() {

    // Trip ID corrente
    private val _currentTripId = MutableLiveData<Long>()

    // LiveData delle foto che si aggiorna automaticamente quando cambia il tripId
    val photos: LiveData<List<PhotoEntity>> = _currentTripId.switchMap { tripId ->
        photoRepository.getPhotosByTripId(tripId)
    }

    fun loadPhotos(tripId: Long) {
        _currentTripId.value = tripId
    }

    fun insertPhoto(tripId: Long, uri: String) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val newPhoto = PhotoEntity(
                        tripId = tripId,
                        uri = uri
                    )
                    photoRepository.insert(newPhoto)
                }
            } catch (e: Exception) {
                Timber.e(e, "Error inserting photo")
            }
        }
    }

    fun deletePhotosWithSystemSync(context: Context, photoIds: List<Long>) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    deletePhotosInternal(context, photoIds, syncWithSystem = true)
                }
            } catch (e: Exception) {
                Timber.e(e, "Error deleting photos")
            }
        }
    }

    fun syncWithSystemGallery(context: Context) {
        val tripId = _currentTripId.value ?: return

        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    syncPhotosWithSystemInternal(context, tripId)
                }
            } catch (e: Exception) {
                Timber.e(e, "Error syncing with system gallery")
            }
        }
    }

    // Private helper methods
    private suspend fun deletePhotosInternal(
        context: Context,
        photoIds: List<Long>,
        syncWithSystem: Boolean
    ) {
        val photosToDelete = photoRepository.getPhotosByIds(photoIds)

        if (syncWithSystem) {
            photosToDelete.forEach { photo ->
                deletePhotoFromSystem(context, photo.uri)
            }
        }

        photoRepository.deletePhotos(photoIds)
    }

    private fun deletePhotoFromSystem(context: Context, uriString: String): Boolean {
        return try {
            val uri = Uri.parse(uriString)
            val rowsDeleted = context.contentResolver.delete(uri, null, null)
            val success = rowsDeleted > 0

            success
        } catch (e: SecurityException) {
            Timber.w(e, "SecurityException deleting from system gallery: $uriString")
            false
        } catch (e: Exception) {
            Timber.e(e, "Error deleting from system gallery: $uriString")
            false
        }
    }

    private suspend fun syncPhotosWithSystemInternal(context: Context, tripId: Long): Int {
        val currentPhotos = photoRepository.getPhotosByTripIdSync(tripId)
        val orphanedPhotoIds = mutableListOf<Long>()

        currentPhotos.forEach { photo ->
            //controllo se la foto è presente nella galleria di sistema
            val isAccessible = try {
                val uri = Uri.parse(photo.uri)
                context.contentResolver.openInputStream(uri)?.use { true } ?: false
            } catch (e: Exception) {
                when (e) {
                    is SecurityException,
                    is FileNotFoundException,
                    is IllegalArgumentException -> false
                    else -> true
                }
            }

            //se non è accessibile, aggiungo alle foto alla lista di foto "orfane"
            if (!isAccessible) {
                orphanedPhotoIds.add(photo.id)
            }
        }

        //elimino le foto orfane: quindi quelle foto che sono state cancellate dalla galleria di sistema
        if (orphanedPhotoIds.isNotEmpty()) {
            photoRepository.deletePhotos(orphanedPhotoIds)
        }

        return orphanedPhotoIds.size
    }
}