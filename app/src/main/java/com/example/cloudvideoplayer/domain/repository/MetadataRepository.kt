package com.example.cloudvideoplayer.domain.repository

import com.example.cloudvideoplayer.domain.model.MediaItem

interface MetadataRepository {
    suspend fun getCachedFolder(path: String): List<MediaItem>?
    suspend fun cacheFolder(path: String, items: List<MediaItem>)
    suspend fun clearCache()
}
