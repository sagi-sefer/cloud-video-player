package com.example.cloudvideoplayer.application

import com.example.cloudvideoplayer.domain.model.MediaItem
import com.example.cloudvideoplayer.domain.repository.CloudStorageRepository
import javax.inject.Inject

class SearchVideosUseCase @Inject constructor(
    private val cloudStorageRepository: CloudStorageRepository
) {
    suspend operator fun invoke(query: String): Result<List<MediaItem.VideoItem>> {
        return cloudStorageRepository.searchVideos(query)
    }
}
