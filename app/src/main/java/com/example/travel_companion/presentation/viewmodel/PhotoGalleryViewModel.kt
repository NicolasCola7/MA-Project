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
                Timber.d("Photo inserted successfully")
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

    private fun deletePhotosFromAppOnly(photoIds: List<Long>) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    photoRepository.deletePhotos(photoIds)
                }
                Timber.d("Deleted ${photoIds.size} photos from app database only")
            } catch (e: Exception) {
                Timber.e(e, "Error deleting photos from app")
            }
        }
    }

    fun syncWithSystemGallery(context: Context) {
        val tripId = _currentTripId.value ?: return

        viewModelScope.launch {
            try {
                val orphanedCount = withContext(Dispatchers.IO) {
                    syncPhotosWithSystemInternal(context, tripId)
                }

                if (orphanedCount > 0) {
                    Timber.d("Removed $orphanedCount orphaned photo references during sync")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error syncing with system gallery")
            }
        }
    }

    /**
     * Controlla se una foto è accessibile e la rimuove automaticamente se non lo è.
     * Usato principalmente per il click sulle foto come fallback di sicurezza.
     */
    fun checkPhotoAccessibilityAndCleanup(context: Context, photo: PhotoEntity): Boolean {
        val isAccessible = try {
            val uri = Uri.parse(photo.uri)
            context.contentResolver.openInputStream(uri)?.use { true } ?: false
        } catch (e: Exception) {
            when (e) {
                is SecurityException,
                is FileNotFoundException,
                is IllegalArgumentException -> {
                    Timber.d("Photo not accessible: ${photo.uri} - ${e.message}")
                    false
                }
                else -> {
                    Timber.e(e, "Unexpected error checking photo accessibility")
                    true // In caso di errore sconosciuto, assumiamo sia accessibile
                }
            }
        }

        // Se non è accessibile, rimuovila automaticamente
        if (!isAccessible) {
            deletePhotosFromAppOnly(listOf(photo.id))
        }

        return isAccessible
    }

    // Private helper methods
    private suspend fun deletePhotosInternal(
        context: Context,
        photoIds: List<Long>,
        syncWithSystem: Boolean
    ) {
        val photosToDelete = photoRepository.getPhotosByIds(photoIds)
        var deletedFromSystem = 0

        if (syncWithSystem) {
            photosToDelete.forEach { photo ->
                if (deletePhotoFromSystem(context, photo.uri)) {
                    deletedFromSystem++
                }
            }
        }

        photoRepository.deletePhotos(photoIds)
        Timber.d("Deleted ${photoIds.size} photos from app database, $deletedFromSystem from system gallery")
    }

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

    private suspend fun syncPhotosWithSystemInternal(context: Context, tripId: Long): Int {
        val currentPhotos = photoRepository.getPhotosByTripIdSync(tripId)
        val orphanedPhotoIds = mutableListOf<Long>()

        currentPhotos.forEach { photo ->
            if (!isPhotoAccessibleInternal(context, photo.uri)) {
                orphanedPhotoIds.add(photo.id)
            }
        }

        if (orphanedPhotoIds.isNotEmpty()) {
            photoRepository.deletePhotos(orphanedPhotoIds)
            Timber.d("Removed ${orphanedPhotoIds.size} orphaned photo references")
        }

        return orphanedPhotoIds.size
    }

    // Metodo interno per la sincronizzazione (senza side effects)
    private fun isPhotoAccessibleInternal(context: Context, uriString: String): Boolean {
        return try {
            val uri = Uri.parse(uriString)
            context.contentResolver.openInputStream(uri)?.use { true } ?: false
        } catch (e: Exception) {
            when (e) {
                is SecurityException,
                is FileNotFoundException,
                is IllegalArgumentException -> false
                else -> true
            }
        }
    }
}