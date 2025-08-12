package com.example.travel_companion.presentation.viewmodel

import android.content.Context
import android.net.Uri
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
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
    private fun groupPhotosByDate(photos: List<PhotoEntity>): List<PhotoGalleryItem> {
        if (photos.isEmpty()) return emptyList()

        // Raggruppa per data usando Calendar
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        val groups = photos
            .sortedByDescending { it.timestamp }
            .groupBy { photo ->
                dateFormat.format(Date(photo.timestamp))
            }
            .map { (dateKey, photosInDate) ->
                val firstPhoto = photosInDate.first()
                PhotoGroup(
                    dateKey = dateKey,
                    timestamp = firstPhoto.timestamp,
                    formattedDate = formatDate(firstPhoto.timestamp),
                    photos = photosInDate
                )
            }
            .sortedByDescending { it.timestamp }

        // Converte in lista di PhotoGalleryItem
        val result = mutableListOf<PhotoGalleryItem>()
        groups.forEach { group ->
            // Aggiungi l'intestazione della data
            result.add(
                PhotoGalleryItem.DateHeader(
                    date = group.dateKey,
                    formattedDate = group.formattedDate,
                    photoCount = group.photos.size
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
    // Alternativa per compatibilità
    private fun formatDate(timestamp: Long): String {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        val today = Calendar.getInstance()
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }

        return when {
            isSameDay(calendar, today) -> "Oggi"
            isSameDay(calendar, yesterday) -> "Ieri"
            else -> SimpleDateFormat("d MMMM yyyy", Locale.getDefault()).format(Date(timestamp))
        }
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
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