package com.example.cloudvideoplayer.application

import com.example.cloudvideoplayer.domain.model.SubtitleItem
import com.example.cloudvideoplayer.domain.repository.CloudStorageRepository
import javax.inject.Inject

class GetExternalSubtitlesUseCase @Inject constructor(
    private val cloudStorageRepository: CloudStorageRepository
) {
    suspend operator fun invoke(folderPath: String): Result<List<SubtitleItem>> {
        return cloudStorageRepository.getExternalSubtitles(folderPath)
    }
}
