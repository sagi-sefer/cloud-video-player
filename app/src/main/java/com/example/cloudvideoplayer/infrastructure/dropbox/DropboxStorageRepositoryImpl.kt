package com.example.cloudvideoplayer.infrastructure.dropbox

import com.dropbox.core.v2.DbxClientV2
import com.dropbox.core.v2.files.FileMetadata
import com.dropbox.core.v2.files.FolderMetadata
import com.example.cloudvideoplayer.domain.model.MediaItem
import com.example.cloudvideoplayer.domain.model.SubtitleItem
import com.example.cloudvideoplayer.domain.model.VideoFormat
import com.example.cloudvideoplayer.domain.repository.CloudStorageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DropboxStorageRepositoryImpl @Inject constructor(
    private val clientProvider: () -> DbxClientV2?
) : CloudStorageRepository {

    private val supportedVideoExtensions = setOf("mkv", "mp4", "webm", "avi", "mov", "m4v")
    private val subtitleExtensions = setOf("srt", "vtt")

    override suspend fun listFolder(path: String): Result<List<MediaItem>> = withContext(Dispatchers.IO) {
        try {
            val client = clientProvider() ?: return@withContext Result.failure(IllegalAccessException("Dropbox client not initialized. Please authenticate."))
            val cleanPath = if (path == "/" || path.isEmpty()) "" else path
            val result = client.files().listFolder(cleanPath)
            val items = mutableListOf<MediaItem>()

            var entries = result.entries
            var hasMore = result.hasMore

            while (true) {
                for (entry in entries) {
                    when (entry) {
                        is FolderMetadata -> {
                            items.add(
                                MediaItem.Folder(
                                    id = entry.id ?: entry.pathLower ?: entry.name,
                                    name = entry.name,
                                    path = entry.pathLower ?: entry.pathDisplay ?: ""
                                )
                            )
                        }
                        is FileMetadata -> {
                            val ext = entry.name.substringAfterLast('.', "").lowercase()
                            if (ext in supportedVideoExtensions) {
                                items.add(
                                    MediaItem.VideoItem(
                                        id = entry.id ?: entry.pathLower ?: entry.name,
                                        name = entry.name,
                                        path = entry.pathLower ?: entry.pathDisplay ?: "",
                                        size = entry.size,
                                        lastModified = entry.serverModified?.time ?: 0L,
                                        format = VideoFormat.fromFileName(entry.name)
                                    )
                                )
                            }
                        }
                        else -> {}
                    }
                }
                if (!hasMore) break
                val continueResult = client.files().listFolderContinue(result.cursor)
                entries = continueResult.entries
                hasMore = continueResult.hasMore
            }

            Result.success(items.sortedBy { it.name.lowercase() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getTemporaryStreamUrl(path: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val client = clientProvider() ?: return@withContext Result.failure(IllegalAccessException("Dropbox client not initialized."))
            val linkResult = client.files().getTemporaryLink(path)
            Result.success(linkResult.link)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getExternalSubtitles(folderPath: String): Result<List<SubtitleItem>> = withContext(Dispatchers.IO) {
        try {
            val client = clientProvider() ?: return@withContext Result.failure(IllegalAccessException("Dropbox client not initialized."))
            val cleanPath = if (folderPath == "/" || folderPath.isEmpty()) "" else folderPath
            val result = client.files().listFolder(cleanPath)
            val subtitles = mutableListOf<SubtitleItem>()

            for (entry in result.entries) {
                if (entry is FileMetadata) {
                    val ext = entry.name.substringAfterLast('.', "").lowercase()
                    if (ext in subtitleExtensions) {
                        val linkResult = client.files().getTemporaryLink(entry.pathLower ?: entry.pathDisplay)
                        subtitles.add(
                            SubtitleItem(
                                id = entry.id ?: entry.pathLower ?: entry.name,
                                name = entry.name,
                                path = entry.pathLower ?: entry.pathDisplay ?: "",
                                downloadUrl = linkResult.link,
                                language = entry.name.substringBeforeLast('.').substringAfterLast('_').ifEmpty { "en" }
                            )
                        )
                    }
                }
            }
            Result.success(subtitles)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun searchVideos(query: String): Result<List<MediaItem.VideoItem>> = withContext(Dispatchers.IO) {
        try {
            val client = clientProvider() ?: return@withContext Result.failure(IllegalAccessException("Dropbox client not initialized."))
            val searchResult = client.files().searchV2(query)
            val videos = mutableListOf<MediaItem.VideoItem>()

            for (match in searchResult.matches) {
                val metadata = match.metadata.metadata
                if (metadata is FileMetadata) {
                    val ext = metadata.name.substringAfterLast('.', "").lowercase()
                    if (ext in supportedVideoExtensions) {
                        videos.add(
                            MediaItem.VideoItem(
                                id = metadata.id ?: metadata.pathLower ?: metadata.name,
                                name = metadata.name,
                                path = metadata.pathLower ?: metadata.pathDisplay ?: "",
                                size = metadata.size,
                                lastModified = metadata.serverModified?.time ?: 0L,
                                format = VideoFormat.fromFileName(metadata.name)
                            )
                        )
                    }
                }
            }
            Result.success(videos)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
