package com.example.cloudvideoplayer.domain.repository

import com.example.cloudvideoplayer.domain.model.MediaItem
import com.example.cloudvideoplayer.domain.model.SubtitleItem

interface CloudStorageRepository {
    suspend fun listFolder(path: String): Result<List<MediaItem>>
    suspend fun getTemporaryStreamUrl(path: String): Result<String>
    suspend fun getExternalSubtitles(folderPath: String): Result<List<SubtitleItem>>
    suspend fun searchVideos(query: String): Result<List<MediaItem.VideoItem>>
}
