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

    // Current trip ID
    private val _currentTripId = MutableLiveData<Long>()

    // LiveData of raw photos from the repository
    private val rawPhotos: LiveData<List<PhotoEntity>> = _currentTripId.switchMap { tripId ->
        photoRepository.getPhotosByTripId(tripId)
    }

    // LiveData of photos grouped by date
    val groupedPhotos: LiveData<List<PhotoGalleryItem>> = rawPhotos.map { photos ->
        groupPhotosByDate(photos)
    }

    /**
     * Sets the current trip ID to load its photos.
     */
    fun loadPhotos(tripId: Long) {
        _currentTripId.value = tripId
    }

    /**
     * Inserts a new photo for a trip.
     */
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

    /**
     * Deletes photos both from repository and system gallery.
     */
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

    /**
     * Syncs repository photos with the system gallery to remove missing files.
     */
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
     * Groups photos by date and creates a list of PhotoGalleryItem.
     */
    private fun groupPhotosByDate(photos: List<PhotoEntity>): List<PhotoGalleryItem> {
        if (photos.isEmpty()) return emptyList()

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        val groups = photos
            .sortedByDescending { it.timestamp }
            .groupBy { photo -> dateFormat.format(Date(photo.timestamp)) }
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

        // Convert groups into PhotoGalleryItem list
        val result = mutableListOf<PhotoGalleryItem>()
        groups.forEach { group ->
            // Add date header
            result.add(
                PhotoGalleryItem.DateHeader(
                    date = group.dateKey,
                    formattedDate = group.formattedDate,
                    photoCount = group.photos.size
                )
            )
            // Add photos in the group
            group.photos.forEach { photo ->
                result.add(PhotoGalleryItem.Photo(photo))
            }
        }

        return result
    }

    /**
     * Formats the timestamp into a user-friendly date.
     */
    private fun formatDate(timestamp: Long): String {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        val today = Calendar.getInstance()
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }

        return when {
            isSameDay(calendar, today) -> "Today"
            isSameDay(calendar, yesterday) -> "Yesterday"
            else -> SimpleDateFormat("d MMMM yyyy", Locale.getDefault()).format(Date(timestamp))
        }
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    // ---------------- Private helper methods ----------------

    /**
     * Deletes photos from both repository and system.
     */
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

    /**
     * Deletes a single photo from the system gallery.
     */
    private fun deletePhotoFromSystem(context: Context, uriString: String): Boolean {
        return try {
            val uri = Uri.parse(uriString)
            val rowsDeleted = context.contentResolver.delete(uri, null, null)
            rowsDeleted > 0
        } catch (e: SecurityException) {
            Timber.w(e, "SecurityException deleting from system gallery: $uriString")
            false
        } catch (e: Exception) {
            Timber.e(e, "Error deleting from system gallery: $uriString")
            false
        }
    }

    /**
     * Checks which photos are missing from system gallery and deletes them from repository.
     */
    private suspend fun syncPhotosWithSystemInternal(context: Context, tripId: Long): Int {
        val currentPhotos = photoRepository.getPhotosByTripIdSync(tripId)
        val orphanedPhotoIds = mutableListOf<Long>()

        currentPhotos.forEach { photo ->
            // Check if photo is accessible in system gallery
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

            // If not accessible, add to orphaned photos list
            if (!isAccessible) {
                orphanedPhotoIds.add(photo.id)
            }
        }

        // Delete orphaned photos from repository
        if (orphanedPhotoIds.isNotEmpty()) {
            photoRepository.deletePhotos(orphanedPhotoIds)
        }

        return orphanedPhotoIds.size
    }
}
