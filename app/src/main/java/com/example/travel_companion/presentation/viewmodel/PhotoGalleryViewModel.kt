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

    /**
     * Elimina le foto sia dal database dell'app che dalla galleria di sistema
     */
    fun deletePhotos(context: Context, photoIds: List<Long>) {
        viewModelScope.launch {
            try {
                // Prima ottieni le foto dal database per avere gli URI
                val photosToDelete = withContext(Dispatchers.IO) {
                    photoRepository.getPhotosByIds(photoIds)
                }

                // Elimina le foto dalla galleria di sistema
                var deletedFromSystem = 0
                withContext(Dispatchers.IO) {
                    photosToDelete.forEach { photo ->
                        if (deletePhotoFromSystem(context, photo.uri)) {
                            deletedFromSystem++
                        }
                    }
                }

                // Poi elimina dal database dell'app
                withContext(Dispatchers.IO) {
                    photoRepository.deletePhotos(photoIds)
                }

                Timber.d("Deleted ${photoIds.size} photos from app database, $deletedFromSystem from system gallery")

            } catch (e: Exception) {
                Timber.e(e, "Error deleting photos")
            }
        }
    }

    /**
     * Metodo backward compatibility per eliminare solo dal database
     */
    fun deletePhotosFromAppOnly(photoIds: List<Long>) {
        viewModelScope.launch(Dispatchers.IO) {
            photoRepository.deletePhotos(photoIds)
        }
    }

    /**
     * Elimina una singola foto dalla galleria di sistema usando MediaStore (Android 10+)
     */
    private fun deletePhotoFromSystem(context: Context, uriString: String): Boolean {
        return try {
            val uri = Uri.parse(uriString)
            val rowsDeleted = context.contentResolver.delete(uri, null, null)
            val success = rowsDeleted > 0

            if (success) {
                Timber.d("Photo deleted from system gallery: $uri")
            } else {
                Timber.w("Failed to delete from system gallery (no rows affected): $uri")
            }

            success
        } catch (e: SecurityException) {
            Timber.w(e, "SecurityException deleting from system gallery: $uriString")
            false
        } catch (e: Exception) {
            Timber.e(e, "Error deleting from system gallery: $uriString")
            false
        }
    }

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