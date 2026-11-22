package com.example.cloudvideoplayer.di

import android.content.Context
import com.example.cloudvideoplayer.infrastructure.player.VlcPlayerManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PlayerModule {

    @Provides
    @Singleton
    fun provideVlcPlayerManager(
        @ApplicationContext context: Context
    ): VlcPlayerManager {
        return VlcPlayerManager(context)
    }
}

