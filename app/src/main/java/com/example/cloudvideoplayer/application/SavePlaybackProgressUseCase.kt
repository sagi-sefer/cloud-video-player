package com.example.cloudvideoplayer.application

import com.example.cloudvideoplayer.domain.model.PlaybackProgress
import com.example.cloudvideoplayer.domain.repository.VideoPlayerRepository
import javax.inject.Inject

class SavePlaybackProgressUseCase @Inject constructor(
    private val videoPlayerRepository: VideoPlayerRepository
) {
    suspend operator fun invoke(progress: PlaybackProgress) {
        videoPlayerRepository.savePlaybackProgress(progress)
    }
}
