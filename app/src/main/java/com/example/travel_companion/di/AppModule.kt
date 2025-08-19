package com.example.travel_companion.di

import android.content.Context
import androidx.room.Room

import com.example.travel_companion.data.local.database.AppDatabase
import com.example.travel_companion.util.TripPredictionEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Singleton
    @Provides
    fun provideDatabase(
        @ApplicationContext app: Context
    ) = Room.databaseBuilder(
        app,
        AppDatabase::class.java,
        "travel_companion_db"
    ).build()

    @Singleton
    @Provides
    fun provideTripDao(db: AppDatabase) = db.tripDao()

    @Singleton
    @Provides
    fun providePhotoDao(db: AppDatabase) = db.photoDao()

    @Singleton
    @Provides
    fun provideCoordinateDao(db: AppDatabase) = db.coordinateDao()

    @Singleton
    @Provides
    fun provideNoteDao(db: AppDatabase) = db.noteDao()

    @Singleton
    @Provides
    fun providePOIDao(db: AppDatabase) = db.poiDao()

    @Provides
    @Singleton
    fun provideTravelPredictionEngine(): TripPredictionEngine {
        return TripPredictionEngine()
    }
}