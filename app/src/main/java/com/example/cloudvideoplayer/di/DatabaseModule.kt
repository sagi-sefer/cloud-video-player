package com.example.cloudvideoplayer.di

import android.content.Context
import androidx.room.Room
import com.example.cloudvideoplayer.domain.repository.VideoPlayerRepository
import com.example.cloudvideoplayer.infrastructure.database.CloudVideoDatabase
import com.example.cloudvideoplayer.infrastructure.database.PlaybackProgressDao
import com.example.cloudvideoplayer.infrastructure.repository.VideoPlayerRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideCloudVideoDatabase(
        @ApplicationContext context: Context
    ): CloudVideoDatabase {
        return Room.databaseBuilder(
            context,
            CloudVideoDatabase::class.java,
            CloudVideoDatabase.DATABASE_NAME
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    @Singleton
    fun providePlaybackProgressDao(database: CloudVideoDatabase): PlaybackProgressDao {
        return database.playbackProgressDao()
    }

    @Provides
    @Singleton
    fun provideVideoPlayerRepository(
        dao: PlaybackProgressDao
    ): VideoPlayerRepository {
        return VideoPlayerRepositoryImpl(dao)
    }
}
