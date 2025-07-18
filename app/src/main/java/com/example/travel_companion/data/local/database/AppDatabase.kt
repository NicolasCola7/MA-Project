package com.example.travel_companion.data.local.database

import androidx.databinding.adapters.Converters
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

// AppDatabase.kt - The main database class
@Database(
    entities = [
        //ExampleEntity::class,
    ],
    version = 1
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    //abstract fun exampleDao(): ExampleDao
}