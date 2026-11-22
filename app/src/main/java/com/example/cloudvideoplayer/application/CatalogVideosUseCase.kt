package com.example.cloudvideoplayer.application

import com.example.cloudvideoplayer.domain.model.MediaItem
import com.example.cloudvideoplayer.domain.repository.CloudStorageRepository
import javax.inject.Inject

class CatalogVideosUseCase @Inject constructor(
    private val cloudStorageRepository: CloudStorageRepository
) {
    suspend operator fun invoke(
        rootPath: String,
        onProgress: (List<MediaItem.VideoItem>) -> Unit = {}
    ): Result<List<MediaItem.VideoItem>> {
        return cloudStorageRepository.catalogVideos(rootPath, onProgress)
    }
}
