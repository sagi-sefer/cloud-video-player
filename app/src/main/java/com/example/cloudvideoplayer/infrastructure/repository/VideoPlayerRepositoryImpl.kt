package com.example.cloudvideoplayer.infrastructure.repository

import com.example.cloudvideoplayer.domain.model.PlaybackProgress
import com.example.cloudvideoplayer.domain.repository.VideoPlayerRepository
import com.example.cloudvideoplayer.infrastructure.database.PlaybackProgressDao
import com.example.cloudvideoplayer.infrastructure.database.PlaybackProgressEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VideoPlayerRepositoryImpl @Inject constructor(
    private val dao: PlaybackProgressDao
) : VideoPlayerRepository {

    override suspend fun getPlaybackProgress(videoId: String): PlaybackProgress? {
        return dao.getProgress(videoId)?.let { entity ->
            PlaybackProgress(
                videoId = entity.videoId,
                positionMs = entity.positionMs,
                durationMs = entity.durationMs,
                updatedAt = entity.updatedAt
            )
        }
    }

    override suspend fun savePlaybackProgress(progress: PlaybackProgress) {
        val entity = PlaybackProgressEntity(
            videoId = progress.videoId,
            positionMs = progress.positionMs,
            durationMs = progress.durationMs,
            updatedAt = progress.updatedAt
        )
        dao.insertOrUpdate(entity)
    }

    override fun observePlaybackProgress(videoId: String): Flow<PlaybackProgress?> {
        return dao.observeProgress(videoId).map { entity ->
            entity?.let {
                PlaybackProgress(
                    videoId = it.videoId,
                    positionMs = it.positionMs,
                    durationMs = it.durationMs,
                    updatedAt = it.updatedAt
                )
            }
        }
    }
}
