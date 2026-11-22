package com.example.cloudvideoplayer.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed class MediaItem : Parcelable {
    abstract val id: String
    abstract val name: String
    abstract val path: String
    abstract val size: Long
    abstract val lastModified: Long

    @Parcelize
    data class Folder(
        override val id: String,
        override val name: String,
        override val path: String,
        override val size: Long = 0L,
        override val lastModified: Long = 0L,
        val itemCount: Int = 0
    ) : MediaItem()

    @Parcelize
    data class VideoItem(
        override val id: String,
        override val name: String,
        override val path: String,
        override val size: Long,
        override val lastModified: Long,
        val durationMs: Long = 0L,
        val format: VideoFormat = VideoFormat.MP4,
        val thumbnailUrl: String? = null,
        val externalSubtitles: List<SubtitleItem> = emptyList()
    ) : MediaItem()
}

@Parcelize
enum class VideoFormat : Parcelable {
    MKV, MP4, WEBM, AVI, MOV, M4V, UNKNOWN;

    companion object {
        fun fromFileName(fileName: String): VideoFormat {
            val ext = fileName.substringAfterLast('.', "").lowercase()
            return when (ext) {
                "mkv" -> MKV
                "mp4" -> MP4
                "webm" -> WEBM
                "avi" -> AVI
                "mov" -> MOV
                "m4v" -> M4V
                else -> UNKNOWN
            }
        }
    }
}

@Parcelize
data class SubtitleItem(
    val id: String,
    val name: String,
    val path: String,
    val downloadUrl: String,
    val language: String = "en"
) : Parcelable

data class PlaybackProgress(
    val videoId: String,
    val positionMs: Long,
    val durationMs: Long,
    val updatedAt: Long = System.currentTimeMillis()
)
