package com.example.cloudvideoplayer.application

import com.example.cloudvideoplayer.domain.model.PlaybackProgress
import com.example.cloudvideoplayer.domain.repository.VideoPlayerRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetPlaybackProgressUseCase @Inject constructor(
    private val videoPlayerRepository: VideoPlayerRepository
) {
    suspend operator fun invoke(videoId: String): PlaybackProgress? {
        return videoPlayerRepository.getPlaybackProgress(videoId)
    }

    fun observe(videoId: String): Flow<PlaybackProgress?> {
        return videoPlayerRepository.observePlaybackProgress(videoId)
    }
}
