package com.example.travel_companion.presentation.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.*
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

    // LiveData foto in base al tripId
    val photos: LiveData<List<PhotoEntity>> = _currentTripId.switchMap { tripId ->
        photoRepository.getPhotosByTripId(tripId)
    }

    // Gestione selezione foto
    private val _selectedPhotosCount = MutableLiveData(0)
    val selectedPhotosCount: LiveData<Int> get() = _selectedPhotosCount

    // Computed properties
    val deleteButtonText: LiveData<String> = selectedPhotosCount.map { count ->
        if (count > 0) "Elimina ($count)" else "Elimina"
    }
    val isDeleteButtonVisible: LiveData<Boolean> = selectedPhotosCount.map { it > 0 }
    val isDeleteButtonEnabled: LiveData<Boolean> = selectedPhotosCount.map { it > 0 }

    fun loadPhotos(tripId: Long) {
        _currentTripId.value = tripId
    }

    fun insertPhoto(tripId: Long, uri: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val newPhoto = PhotoEntity(tripId = tripId, uri = uri)
                photoRepository.insert(newPhoto)
                Timber.d("Foto salvata: $uri")
            } catch (e: Exception) {
                Timber.e(e, "Errore nel salvataggio della foto")
            }
        }
    }

    fun deletePhotosWithSystemSync(context: Context, photoIds: List<Long>) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                deletePhotosInternal(context, photoIds, syncWithSystem = true)
                clearSelection()
            } catch (e: Exception) {
                Timber.e(e, "Errore durante l'eliminazione delle foto")
            }
        }
    }

    fun deletePhotosFromAppOnly(photoIds: List<Long>) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                photoRepository.deletePhotos(photoIds)
                Timber.d("Eliminate ${photoIds.size} foto dal database interno")
            } catch (e: Exception) {
                Timber.e(e, "Errore durante l'eliminazione delle foto dall'app")
            }
        }
    }

    fun syncWithSystemGallery(context: Context) {
        val tripId = _currentTripId.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val orphanedCount = syncPhotosWithSystemInternal(context, tripId)
                if (orphanedCount > 0) {
                    Timber.d("Rimossi $orphanedCount riferimenti a foto non disponibili")
                }
            } catch (e: Exception) {
                Timber.e(e, "Errore durante la sincronizzazione")
            }
        }
    }

    fun updateSelectedCount(count: Int) {
        _selectedPhotosCount.postValue(count)
    }

    fun clearSelection() {
        _selectedPhotosCount.postValue(0)
    }

    fun isPhotoAccessible(context: Context, uriString: String): Boolean {
        return try {
            val uri = Uri.parse(uriString)
            context.contentResolver.openInputStream(uri)?.use { true } ?: false
        } catch (e: Exception) {
            when (e) {
                is SecurityException,
                is FileNotFoundException,
                is IllegalArgumentException -> {
                    Timber.d("Foto non accessibile: $uriString - ${e.message}")
                    false
                }
                else -> {
                    Timber.e(e, "Errore inaspettato controllando accessibilità foto")
                    true
                }
            }
        }
    }

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
        Timber.d("Eliminate ${photoIds.size} foto (di cui $deletedFromSystem anche dalla galleria di sistema)")
    }

    /**
     * Elimina una singola foto dalla galleria di sistema usando MediaStore (Android 10+)
     */
    private fun deletePhotoFromSystem(context: Context, uriString: String): Boolean {
        return try {
            val uri = Uri.parse(uriString)
            val rowsDeleted = context.contentResolver.delete(uri, null, null)
            rowsDeleted > 0
        } catch (e: SecurityException) {
            Timber.w(e, "SecurityException eliminando dalla galleria di sistema: $uriString")
            false
        } catch (e: Exception) {
            Timber.e(e, "Errore eliminando dalla galleria di sistema: $uriString")
            false
        }
    }

    private suspend fun syncPhotosWithSystemInternal(context: Context, tripId: Long): Int {
        val currentPhotos = photoRepository.getPhotosByTripIdSync(tripId)
        val orphanedPhotoIds = mutableListOf<Long>()

        currentPhotos.forEach { photo ->
            if (!isPhotoAccessible(context, photo.uri)) {
                orphanedPhotoIds.add(photo.id)
            }
        }

        if (orphanedPhotoIds.isNotEmpty()) {
            photoRepository.deletePhotos(orphanedPhotoIds)
            Timber.d("Rimossi ${orphanedPhotoIds.size} riferimenti a foto orfane")
        }

        return orphanedPhotoIds.size
    }
}
