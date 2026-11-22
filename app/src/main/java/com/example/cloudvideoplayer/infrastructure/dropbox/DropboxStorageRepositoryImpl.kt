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
class DropboxClientProvider @Inject constructor(
    private val prefs: android.content.SharedPreferences,
    private val requestConfig: com.dropbox.core.DbxRequestConfig
) {
    operator fun invoke(): DbxClientV2? {
        val token = prefs.getString("dropbox_access_token", null)
        return if (!token.isNullOrBlank()) {
            DbxClientV2(requestConfig, token)
        } else {
            null
        }
    }
}

@Singleton
class DropboxStorageRepositoryImpl @Inject constructor(
    private val clientProvider: DropboxClientProvider
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

    override suspend fun catalogVideos(
        rootPath: String,
        onProgress: (List<MediaItem.VideoItem>) -> Unit
    ): Result<List<MediaItem.VideoItem>> = withContext(Dispatchers.IO) {
        try {
            val cleanPath = when {
                rootPath == "/" || rootPath.isEmpty() -> ""
                !rootPath.startsWith("/") -> "/$rootPath"
                else -> rootPath
            }
            android.util.Log.d("DropboxRepo", "Starting catalogVideos for path: '$cleanPath'")
            val client = clientProvider() ?: return@withContext Result.failure(IllegalAccessException("Dropbox client not initialized. Please authenticate."))
            val videos = mutableListOf<MediaItem.VideoItem>()

            // 1. Try fast Dropbox Search API first
            try {
                val searchResult = client.files().searchV2Builder("mkv mp4 avi mov m4v webm")
                    .withOptions(
                        com.dropbox.core.v2.files.SearchOptions.newBuilder()
                            .withPath(if (cleanPath.isEmpty()) null else cleanPath)
                            .withMaxResults(500L)
                            .build()
                    )
                    .start()

                for (match in searchResult.matches) {
                    val meta = match.metadata
                    if (meta.isMetadata) {
                        val fileVal = meta.metadataValue
                        if (fileVal is FileMetadata) {
                            val ext = fileVal.name.substringAfterLast('.', "").lowercase()
                            if (ext in supportedVideoExtensions) {
                                val fullPath = fileVal.pathLower ?: fileVal.pathDisplay ?: ""
                                val parentFolder = fullPath.substringBeforeLast('/')
                                val movieFolderName = parentFolder.substringAfterLast('/').ifEmpty { fileVal.name.substringBeforeLast('.') }
                                videos.add(
                                    MediaItem.VideoItem(
                                        id = fileVal.id ?: fullPath,
                                        name = movieFolderName, // 02 level folder name!
                                        path = fullPath,
                                        size = fileVal.size,
                                        lastModified = fileVal.serverModified?.time ?: 0L,
                                        format = VideoFormat.fromFileName(fileVal.name)
                                    )
                                )
                            }
                        }
                    }
                }
                if (videos.isNotEmpty()) {
                    val sorted = videos.distinctBy { it.id }.sortedBy { it.name.lowercase() }
                    android.util.Log.d("DropboxRepo", "SearchV2 found ${sorted.size} videos instantly!")
                    onProgress(sorted)
                    return@withContext Result.success(sorted)
                }
            } catch (e: Exception) {
                android.util.Log.w("DropboxRepo", "SearchV2 fallback to recursive listFolder", e)
            }

            // 2. Fallback to listFolder withRecursive with live progress updates
            val result = client.files().listFolderBuilder(cleanPath).withRecursive(true).start()
            var entries = result.entries
            var hasMore = result.hasMore
            var cursor = result.cursor

            while (true) {
                for (entry in entries) {
                    if (entry is FileMetadata) {
                        val ext = entry.name.substringAfterLast('.', "").lowercase()
                        if (ext in supportedVideoExtensions) {
                            val fullPath = entry.pathLower ?: entry.pathDisplay ?: ""
                            val parentFolder = fullPath.substringBeforeLast('/')
                            val movieFolderName = parentFolder.substringAfterLast('/').ifEmpty { entry.name.substringBeforeLast('.') }
                            videos.add(
                                MediaItem.VideoItem(
                                    id = entry.id ?: fullPath,
                                    name = movieFolderName, // 02 level folder name!
                                    path = fullPath,
                                    size = entry.size,
                                    lastModified = entry.serverModified?.time ?: 0L,
                                    format = VideoFormat.fromFileName(entry.name)
                                )
                            )
                        }
                    }
                }
                val currentList = videos.distinctBy { it.id }.sortedBy { it.name.lowercase() }
                onProgress(currentList)
                if (!hasMore) break
                val continueResult = client.files().listFolderContinue(cursor)
                entries = continueResult.entries
                hasMore = continueResult.hasMore
                cursor = continueResult.cursor
            }

            val finalVideos = videos.distinctBy { it.id }.sortedBy { it.name.lowercase() }
            android.util.Log.d("DropboxRepo", "Finished cataloging ${finalVideos.size} videos")
            Result.success(finalVideos)
        } catch (e: Exception) {
            android.util.Log.e("DropboxRepo", "Error cataloging videos for '$rootPath'", e)
            Result.failure(e)
        }
    }

    override suspend fun searchVideos(query: String): Result<List<MediaItem.VideoItem>> = withContext(Dispatchers.IO) {
        try {
            clientProvider() ?: return@withContext Result.failure(IllegalAccessException("Dropbox client not initialized."))
            Result.success(emptyList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
