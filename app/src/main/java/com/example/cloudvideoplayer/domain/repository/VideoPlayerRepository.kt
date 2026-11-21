package com.example.cloudvideoplayer.domain.repository

import com.example.cloudvideoplayer.domain.model.PlaybackProgress
import kotlinx.coroutines.flow.Flow

interface VideoPlayerRepository {
    suspend fun getPlaybackProgress(videoId: String): PlaybackProgress?
    suspend fun savePlaybackProgress(progress: PlaybackProgress)
    fun observePlaybackProgress(videoId: String): Flow<PlaybackProgress?>
}
