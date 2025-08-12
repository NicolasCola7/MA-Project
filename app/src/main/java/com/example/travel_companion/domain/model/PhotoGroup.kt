package com.example.travel_companion.domain.model

import com.example.travel_companion.data.local.entity.PhotoEntity
import java.time.LocalDate

/**
 * Sealed class per rappresentare gli elementi della lista raggruppata
 */
sealed class PhotoGalleryItem {
    data class DateHeader(
        val date: LocalDate,
        val formattedDate: String,
        val photoCount: Int
    ) : PhotoGalleryItem()

    data class Photo(
        val photoEntity: PhotoEntity
    ) : PhotoGalleryItem()
}

/**
 * Data class per raggruppare le foto per data
 */
data class PhotoGroup(
    val date: LocalDate,
    val formattedDate: String,
    val photos: List<PhotoEntity>
) {
    val photoCount: Int get() = photos.size
}