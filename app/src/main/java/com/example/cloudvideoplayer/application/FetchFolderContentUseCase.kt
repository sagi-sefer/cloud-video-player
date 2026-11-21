package com.example.cloudvideoplayer.application

import com.example.cloudvideoplayer.domain.model.MediaItem
import com.example.cloudvideoplayer.domain.repository.CloudStorageRepository
import javax.inject.Inject

class FetchFolderContentUseCase @Inject constructor(
    private val cloudStorageRepository: CloudStorageRepository
) {
    suspend operator fun invoke(path: String): Result<List<MediaItem>> {
        return cloudStorageRepository.listFolder(path)
    }
}
