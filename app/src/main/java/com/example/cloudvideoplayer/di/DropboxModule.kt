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
    private const val KEY_ACCESS_TOKEN = "dropbox_access_token"

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
    fun provideDropboxClientProvider(
        prefs: SharedPreferences,
        requestConfig: DbxRequestConfig
    ): () -> DbxClientV2? = {
        val token = prefs.getString(KEY_ACCESS_TOKEN, null)
        if (!token.isNullOrBlank()) {
            DbxClientV2(requestConfig, token)
        } else {
            // Fallback or empty if not authenticated yet
            null
        }
    }

    @Provides
    @Singleton
    fun provideCloudStorageRepository(
        clientProvider: () -> DbxClientV2?
    ): CloudStorageRepository {
        return DropboxStorageRepositoryImpl(clientProvider)
    }
}
