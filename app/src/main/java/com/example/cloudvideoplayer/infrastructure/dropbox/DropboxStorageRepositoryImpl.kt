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
            val cleanPath = when {
                path == "/" || path.isEmpty() -> ""
                !path.startsWith("/") -> "/$path"
                else -> path
            }
            val result = client.files().listFolder(cleanPath)
            val items = mutableListOf<MediaItem>()

            var entries = result.entries
            var hasMore = result.hasMore

            val files = mutableListOf<FileMetadata>()
            val folders = mutableListOf<FolderMetadata>()

            while (true) {
                for (entry in entries) {
                    when (entry) {
                        is FolderMetadata -> folders.add(entry)
                        is FileMetadata -> files.add(entry)
                        else -> {}
                    }
                }
                if (!hasMore) break
                val continueResult = client.files().listFolderContinue(result.cursor)
                entries = continueResult.entries
                hasMore = continueResult.hasMore
            }

            val imageExtensions = setOf("jpg", "jpeg")
            val imageMap = files
                .filter { it.name.substringAfterLast('.', "").lowercase() in imageExtensions }
                .associateBy { it.name.substringBeforeLast('.').lowercase() }

            for (folder in folders) {
                val folderBaseName = folder.name.lowercase()
                val parentImageFile = imageMap[folderBaseName]
                val imagePath = if (parentImageFile != null) {
                    parentImageFile.pathLower ?: parentImageFile.pathDisplay
                } else {
                    try {
                        val subfolderResult = client.files().listFolder(folder.pathLower ?: folder.pathDisplay ?: "")
                        val subfolderImage = subfolderResult.entries
                            .filterIsInstance<FileMetadata>()
                            .firstOrNull { it.name.substringAfterLast('.', "").lowercase() in imageExtensions }
                        subfolderImage?.pathLower ?: subfolderImage?.pathDisplay
                    } catch (_: Exception) {
                        null
                    }
                }

                val thumbnailUrl = if (imagePath != null) {
                    try {
                        client.files().getTemporaryLink(imagePath).link
                    } catch (_: Exception) {
                        null
                    }
                } else {
                    null
                }

                items.add(
                    MediaItem.Folder(
                        id = folder.id ?: folder.pathLower ?: folder.name,
                        name = folder.name,
                        path = folder.pathLower ?: folder.pathDisplay ?: "",
                        thumbnailUrl = thumbnailUrl
                    )
                )
            }

            for (file in files) {
                val ext = file.name.substringAfterLast('.', "").lowercase()
                if (ext in supportedVideoExtensions) {
                    val videoName = file.name
                    val baseName = videoName.substringBeforeLast('.').lowercase()
                    val imageFile = imageMap[baseName] ?: imageMap.values.firstOrNull()
                    val imagePath = imageFile?.pathLower ?: imageFile?.pathDisplay

                    val thumbnailUrl = if (imagePath != null) {
                        try {
                            client.files().getTemporaryLink(imagePath).link
                        } catch (_: Exception) {
                            null
                        }
                    } else {
                        null
                    }

                    items.add(
                        MediaItem.VideoItem(
                            id = file.id ?: file.pathLower ?: file.name,
                            name = videoName,
                            path = file.pathLower ?: file.pathDisplay ?: "",
                            size = file.size,
                            lastModified = file.serverModified?.time ?: 0L,
                            format = VideoFormat.fromFileName(videoName),
                            thumbnailUrl = thumbnailUrl
                        )
                    )
                }
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

            // Fallback to listFolder withRecursive with live progress updates and thumbnail association
            val result = client.files().listFolderBuilder(cleanPath).withRecursive(true).start()
            var entries = result.entries
            var hasMore = result.hasMore
            var cursor = result.cursor

            val allFiles = mutableListOf<FileMetadata>()
            while (true) {
                for (entry in entries) {
                    if (entry is FileMetadata) {
                        allFiles.add(entry)
                    }
                }
                if (!hasMore) break
                val continueResult = client.files().listFolderContinue(cursor)
                entries = continueResult.entries
                hasMore = continueResult.hasMore
                cursor = continueResult.cursor
            }

            val imageExtensions = setOf("jpg", "jpeg")
            val imageMapByFolder = allFiles
                .filter { it.name.substringAfterLast('.', "").lowercase() in imageExtensions }
                .groupBy { it.pathLower?.substringBeforeLast('/') ?: "" }
                .mapValues { (_, imgList) ->
                    imgList.associateBy { it.name.substringBeforeLast('.').lowercase() }
                }

            for (entry in allFiles) {
                val ext = entry.name.substringAfterLast('.', "").lowercase()
                if (ext in supportedVideoExtensions) {
                    val fullPath = entry.pathLower ?: entry.pathDisplay ?: ""
                    val parentFolder = fullPath.substringBeforeLast('/')
                    val movieFolderName = parentFolder.substringAfterLast('/').ifEmpty { entry.name.substringBeforeLast('.') }
                    val videoBaseName = entry.name.substringBeforeLast('.').lowercase()
                    val folderImages = imageMapByFolder[parentFolder]
                    val imageFile = folderImages?.get(videoBaseName) ?: folderImages?.values?.firstOrNull()
                    val imagePath = imageFile?.pathLower ?: imageFile?.pathDisplay

                    val thumbnailUrl = if (imagePath != null) {
                        try {
                            client.files().getTemporaryLink(imagePath).link
                        } catch (_: Exception) {
                            null
                        }
                    } else {
                        null
                    }

                    videos.add(
                        MediaItem.VideoItem(
                            id = entry.id ?: fullPath,
                            name = movieFolderName,
                            path = fullPath,
                            size = entry.size,
                            lastModified = entry.serverModified?.time ?: 0L,
                            format = VideoFormat.fromFileName(entry.name),
                            thumbnailUrl = thumbnailUrl
                        )
                    )
                }
                val currentList = videos.distinctBy { it.id }.sortedBy { it.name.lowercase() }
                onProgress(currentList)
            }

            val finalVideos = videos.distinctBy { it.id }.sortedBy { it.name.lowercase() }
            android.util.Log.d("DropboxRepo", "Finished cataloging ${finalVideos.size} videos with thumbnails")
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
