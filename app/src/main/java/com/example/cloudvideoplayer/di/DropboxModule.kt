package com.example.cloudvideoplayer.di

import android.content.Context
import android.content.SharedPreferences
import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.v2.DbxClientV2
import com.example.cloudvideoplayer.BuildConfig
import com.example.cloudvideoplayer.domain.repository.CloudStorageRepository
import com.example.cloudvideoplayer.infrastructure.dropbox.DropboxStorageRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DropboxModule {

    private const val PREFS_NAME = "cloud_video_prefs"

    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    @Provides
    @Singleton
    fun provideDbxRequestConfig(): DbxRequestConfig {
        return DbxRequestConfig.newBuilder("CloudVideoPlayer/1.0").build()
    }

    @Provides
    @Singleton
    fun provideCloudStorageRepository(
        clientProvider: com.example.cloudvideoplayer.infrastructure.dropbox.DropboxClientProvider
    ): CloudStorageRepository {
        return DropboxStorageRepositoryImpl(clientProvider)
    }
}
