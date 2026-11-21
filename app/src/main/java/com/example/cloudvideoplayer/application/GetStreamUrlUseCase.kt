package com.example.cloudvideoplayer.application

import com.example.cloudvideoplayer.domain.repository.CloudStorageRepository
import javax.inject.Inject

class GetStreamUrlUseCase @Inject constructor(
    private val cloudStorageRepository: CloudStorageRepository
) {
    suspend operator fun invoke(path: String): Result<String> {
        return cloudStorageRepository.getTemporaryStreamUrl(path)
    }
}
