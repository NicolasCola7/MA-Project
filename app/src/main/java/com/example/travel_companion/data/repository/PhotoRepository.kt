package com.example.travel_companion.data.repository

import androidx.lifecycle.LiveData
import com.example.travel_companion.data.local.dao.PhotoDao
import com.example.travel_companion.data.local.entity.PhotoEntity
import javax.inject.Inject

class PhotoRepository @Inject constructor (
    private val photoDao: PhotoDao
)  {

    suspend fun insert(photo: PhotoEntity) {
        photoDao.insert(photo);
    }

    fun getPhotosByTripId(tripId: Long): LiveData<List<PhotoEntity>> {
        return photoDao.getPhotosByTripId(tripId)
    }

}