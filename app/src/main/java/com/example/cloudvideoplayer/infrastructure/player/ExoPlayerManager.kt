package com.example.cloudvideoplayer.infrastructure.player

import android.content.Context
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.MediaItem as Media3Item
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.ExoPlayer
import com.example.cloudvideoplayer.domain.model.SubtitleItem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExoPlayerManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val exoPlayer: ExoPlayer = ExoPlayer.Builder(context).build().apply {
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

    fun setAudioTrack(groupIndex: Int, trackIndex: Int) {
        val parameters = exoPlayer.trackSelectionParameters
            .buildUpon()
            // Custom track selection logic using TrackSelectionParameters
            .build()
        exoPlayer.trackSelectionParameters = parameters
    }

    fun setSubtitleEnabled(enabled: Boolean) {
        val parameters = exoPlayer.trackSelectionParameters
            .buildUpon()
            .setDisabledTextTrackSelection(!enabled)
            .build()
        exoPlayer.trackSelectionParameters = parameters
    }

    fun updatePosition() {
        _currentPosition.value = exoPlayer.currentPosition
        _duration.value = exoPlayer.duration.coerceAtLeast(0L)
    }

    fun release() {
        exoPlayer.release()
    }
}
