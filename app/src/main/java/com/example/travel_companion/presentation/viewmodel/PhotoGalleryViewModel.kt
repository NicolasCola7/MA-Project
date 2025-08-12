package com.example.travel_companion.presentation.viewmodel

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.switchMap
import androidx.lifecycle.map
import com.example.travel_companion.data.local.entity.PhotoEntity
import com.example.travel_companion.data.repository.PhotoRepository
import com.example.travel_companion.domain.model.PhotoGalleryItem
import com.example.travel_companion.domain.model.PhotoGroup
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.FileNotFoundException
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class PhotoGalleryViewModel @Inject constructor(
    private val photoRepository: PhotoRepository
) : ViewModel() {

    // Trip ID corrente
    private val _currentTripId = MutableLiveData<Long>()

    // LiveData delle foto grezze dal repository
    private val rawPhotos: LiveData<List<PhotoEntity>> = _currentTripId.switchMap { tripId ->
        photoRepository.getPhotosByTripId(tripId)
    }

    // LiveData delle foto raggruppate per data
    @RequiresApi(Build.VERSION_CODES.O)
    val groupedPhotos: LiveData<List<PhotoGalleryItem>> = rawPhotos.map { photos ->
        groupPhotosByDate(photos)
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
                        uri = uri,
                        timestamp = System.currentTimeMillis()
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
                    deletePhotosInternal(context, photoIds)
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

    /**
     * Raggruppa le foto per data e crea la lista di PhotoGalleryItem
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun groupPhotosByDate(photos: List<PhotoEntity>): List<PhotoGalleryItem> {
        if (photos.isEmpty()) return emptyList()

        val groups = photos
            .sortedByDescending { it.timestamp } // Più recenti prima
            .groupBy { photo ->
                Instant.ofEpochMilli(photo.timestamp)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
            }
            .map { (date, photosInDate) ->
                PhotoGroup(
                    date = date,
                    formattedDate = formatDate(date),
                    photos = photosInDate
                )
            }
            .sortedByDescending { it.date } // Giorni più recenti prima

        // Converte in lista di PhotoGalleryItem
        val result = mutableListOf<PhotoGalleryItem>()
        groups.forEach { group ->
            // Aggiungi l'intestazione della data
            result.add(
                PhotoGalleryItem.DateHeader(
                    date = group.date,
                    formattedDate = group.formattedDate,
                    photoCount = group.photoCount
                )
            )
            // Aggiungi le foto del gruppo
            group.photos.forEach { photo ->
                result.add(PhotoGalleryItem.Photo(photo))
            }
        }

        return result
    }

    /**
     * Formatta la data in modo user-friendly
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun formatDate(date: LocalDate): String {
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)

        return when (date) {
            today -> "Oggi"
            yesterday -> "Ieri"
            else -> {
                if (date.year == today.year) {
                    // Stesso anno: "15 marzo"
                    "${date.dayOfMonth} ${date.month.getDisplayName(TextStyle.FULL, Locale.getDefault())}"
                } else {
                    // Anno diverso: "15 marzo 2023"
                    date.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.getDefault()))
                }
            }
        }
    }

    // Private helper methods
    private suspend fun deletePhotosInternal(
        context: Context,
        photoIds: List<Long>
    ) {
        val photosToDelete = photoRepository.getPhotosByIds(photoIds)

        photosToDelete.forEach { photo ->
            deletePhotoFromSystem(context, photo.uri)
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