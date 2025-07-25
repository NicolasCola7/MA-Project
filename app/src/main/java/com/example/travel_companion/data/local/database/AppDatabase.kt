package com.example.travel_companion.data.local.database

import androidx.databinding.adapters.Converters
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.travel_companion.data.local.dao.*
import com.example.travel_companion.data.local.entity.*

// AppDatabase.kt - The main database class
@Database(
    entities = [
        TripEntity::class,
        CoordinateEntity::class,
        PhotoEntity::class,
        NoteEntity::class
    ],
    version = 1
)

abstract class AppDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao
    abstract fun coordinateDao(): CoordinateDao
    abstract fun photoDao(): PhotoDao
    abstract fun noteDao(): NoteDao
}