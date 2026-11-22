package com.example.cloudvideoplayer.infrastructure.player

import android.content.Context
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.MediaItem as Media3Item
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.extractor.DefaultExtractorsFactory
import com.example.cloudvideoplayer.domain.model.SubtitleItem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class TrackInfo(
    val groupIndex: Int,
    val trackIndex: Int,
    val name: String,
    val isSelected: Boolean,
    val isSupported: Boolean = true
)

@Singleton
class ExoPlayerManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val trackSelector = DefaultTrackSelector(context).apply {
        setParameters(
            buildUponParameters()
                .setPreferredAudioMimeTypes(
                    MimeTypes.AUDIO_AAC,
                    MimeTypes.AUDIO_MPEG,
                    MimeTypes.AUDIO_OPUS,
                    MimeTypes.AUDIO_VORBIS,
                    MimeTypes.AUDIO_RAW
                )
        )
    }

    private val renderersFactory = DefaultRenderersFactory(context)

    private val extractorsFactory = DefaultExtractorsFactory().apply {
        setTextTrackTranscodingEnabled(true)
    }

    private val mediaSourceFactory = DefaultMediaSourceFactory(context, extractorsFactory)

    val exoPlayer: ExoPlayer = ExoPlayer.Builder(context, renderersFactory, mediaSourceFactory)
        .setTrackSelector(trackSelector)
        .build()
        .apply {
            playWhenReady = true
        }

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _tracksInfo = MutableStateFlow<Tracks?>(null)
    val tracksInfo: StateFlow<Tracks?> = _tracksInfo.asStateFlow()

    init {
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                _duration.value = exoPlayer.duration.coerceAtLeast(0L)
            }

            override fun onTracksChanged(tracks: Tracks) {
                _tracksInfo.value = tracks
            }

            override fun onPlayerError(error: PlaybackException) {
                android.util.Log.e("ExoPlayerManager", "Player error encountered: ${error.message}", error)
                if (error.errorCode == PlaybackException.ERROR_CODE_DECODER_INIT_FAILED) {
                    exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                        .buildUpon()
                        .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
                        .build()
                    exoPlayer.prepare()
                    exoPlayer.play()
                } else {
                    exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                        .buildUpon()
                        .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                        .build()
                    exoPlayer.prepare()
                    exoPlayer.play()
                }
            }
        })
    }

    fun prepareAndPlay(videoUrl: String, subtitles: List<SubtitleItem> = emptyList(), startPositionMs: Long = 0L) {
        val mediaItemBuilder = Media3Item.Builder()
            .setUri(Uri.parse(videoUrl))

        if (subtitles.isNotEmpty()) {
            val subtitleConfigs = subtitles.map { sub ->
                val mimeType = if (sub.name.endsWith(".vtt", ignoreCase = true)) {
                    MimeTypes.TEXT_VTT
                } else {
                    MimeTypes.APPLICATION_SUBRIP
                }
                Media3Item.SubtitleConfiguration.Builder(Uri.parse(sub.downloadUrl))
                    .setMimeType(mimeType)
                    .setLanguage(sub.language)
                    .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                    .setRoleFlags(C.ROLE_FLAG_SUBTITLE)
                    .build()
            }
            mediaItemBuilder.setSubtitleConfigurations(subtitleConfigs)
        }

        val mediaItem = mediaItemBuilder.build()
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        if (startPositionMs > 0) {
            exoPlayer.seekTo(startPositionMs)
        }
        exoPlayer.play()
    }

    fun pause() {
        exoPlayer.pause()
    }

    fun resume() {
        exoPlayer.play()
    }

    fun seekTo(positionMs: Long) {
        exoPlayer.seekTo(positionMs)
    }

    fun getAudioTracksInfo(): List<TrackInfo> {
        val tracks = _tracksInfo.value ?: return emptyList()
        val list = mutableListOf<TrackInfo>()
        for (group in tracks.groups) {
            if (group.type == C.TRACK_TYPE_AUDIO) {
                for (i in 0 until group.length) {
                    val format = group.getTrackFormat(i)
                    val lang = format.label ?: format.language ?: "Audio Track ${i + 1}"
                    val mime = format.sampleMimeType?.substringAfter('/')?.uppercase() ?: ""
                    val channels = if (format.channelCount > 0) "${format.channelCount}ch" else ""
                    val nameStr = buildString {
                        append(lang)
                        if (mime.isNotEmpty() || channels.isNotEmpty()) {
                            append(" (").append(listOf(mime, channels).filter { it.isNotEmpty() }.joinToString(", ")).append(")")
                        }
                    }
                    list.add(
                        TrackInfo(
                            groupIndex = tracks.groups.indexOf(group),
                            trackIndex = i,
                            name = nameStr,
                            isSelected = group.isTrackSelected(i),
                            isSupported = group.isTrackSupported(i)
                        )
                    )
                }
            }
        }
        return list
    }

    fun getSubtitleTracksInfo(): List<TrackInfo> {
        val tracks = _tracksInfo.value ?: return emptyList()
        val list = mutableListOf<TrackInfo>()
        for (group in tracks.groups) {
            if (group.type == C.TRACK_TYPE_TEXT) {
                for (i in 0 until group.length) {
                    val format = group.getTrackFormat(i)
                    val lang = format.label ?: format.language ?: "Subtitle ${i + 1}"
                    val mime = format.sampleMimeType?.substringAfter('/')?.uppercase() ?: ""
                    val nameStr = if (mime.isNotEmpty()) "$lang ($mime)" else lang
                    list.add(
                        TrackInfo(
                            groupIndex = tracks.groups.indexOf(group),
                            trackIndex = i,
                            name = nameStr,
                            isSelected = group.isTrackSelected(i),
                            isSupported = group.isTrackSupported(i)
                        )
                    )
                }
            }
        }
        return list
    }

    fun setSubtitleEnabled(enabled: Boolean) {
        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
            .buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, !enabled)
            .clearOverridesOfType(C.TRACK_TYPE_TEXT)
            .build()
    }

    fun selectTrack(groupIndex: Int, trackIndex: Int): Boolean {
        val tracks = _tracksInfo.value ?: return false
        val group = tracks.groups.getOrNull(groupIndex) ?: return false
        val trackType = group.type
        val format = group.mediaTrackGroup.getFormat(trackIndex)

        val override = TrackSelectionOverride(group.mediaTrackGroup, listOf(trackIndex))
        val builder = exoPlayer.trackSelectionParameters
            .buildUpon()
            .clearOverridesOfType(trackType)
            .addOverride(override)

        if (trackType == C.TRACK_TYPE_TEXT) {
            builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
            if (!format.language.isNullOrBlank()) {
                builder.setPreferredTextLanguage(format.language)
            }
        }

        val newParams = builder.build()
        exoPlayer.trackSelectionParameters = newParams
        exoPlayer.play()
        return true
    }

    fun updatePosition() {
        _currentPosition.value = exoPlayer.currentPosition
        _duration.value = exoPlayer.duration.coerceAtLeast(0L)
    }

    fun release() {
        exoPlayer.release()
    }
}
