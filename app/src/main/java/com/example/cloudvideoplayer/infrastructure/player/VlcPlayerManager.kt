package com.example.cloudvideoplayer.infrastructure.player

import android.content.Context
import android.net.Uri
import com.example.cloudvideoplayer.domain.model.SubtitleItem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import javax.inject.Inject
import javax.inject.Singleton

data class VlcTrackInfo(
    val id: Int,
    val name: String,
    val isSelected: Boolean
)

@Singleton
class VlcPlayerManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val libVLC: LibVLC = LibVLC(context, ArrayList<String>().apply {
        add("--no-drop-late-frames")
        add("--no-skip-frames")
        add("--rtsp-tcp")
        add("-vvv")
    })

    val mediaPlayer: MediaPlayer = MediaPlayer(libVLC).apply {
        setEventListener { event ->
            when (event.type) {
                MediaPlayer.Event.Playing -> _isPlaying.value = true
                MediaPlayer.Event.Paused -> _isPlaying.value = false
                MediaPlayer.Event.Stopped -> _isPlaying.value = false
                MediaPlayer.Event.EndReached -> _isPlaying.value = false
                MediaPlayer.Event.TimeChanged -> {
                    _currentPosition.value = time
                }
                MediaPlayer.Event.LengthChanged -> {
                    _duration.value = length
                }
                else -> {}
            }
        }
    }

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    fun prepareAndPlay(videoUrl: String, subtitles: List<SubtitleItem> = emptyList(), startPositionMs: Long = 0L) {
        try {
            val media = Media(libVLC, Uri.parse(videoUrl))
            media.parse()

            // Add external subtitles if present
            for (sub in subtitles) {
                try {
                    media.addOption(":sub-file=${sub.downloadUrl}")
                } catch (e: Exception) {
                    android.util.Log.e("VlcPlayerManager", "Failed to add external subtitle: ${sub.name}", e)
                }
            }

            mediaPlayer.media = media
            media.release()

            mediaPlayer.play()
            if (startPositionMs > 0) {
                mediaPlayer.time = startPositionMs
            }
        } catch (e: Exception) {
            android.util.Log.e("VlcPlayerManager", "Error preparing media", e)
        }
    }

    fun pause() {
        if (mediaPlayer.isPlaying) {
            mediaPlayer.pause()
        }
    }

    fun resume() {
        if (!mediaPlayer.isPlaying) {
            mediaPlayer.play()
        }
    }

    fun seekTo(positionMs: Long) {
        mediaPlayer.time = positionMs
    }

    fun getAudioTracksInfo(): List<VlcTrackInfo> {
        val tracks = mediaPlayer.audioTracks ?: return emptyList()
        val currentId = mediaPlayer.audioTrack
        val list = mutableListOf<VlcTrackInfo>()
        for (track in tracks) {
            list.add(
                VlcTrackInfo(
                    id = track.id,
                    name = track.name ?: "Audio ${track.id}",
                    isSelected = (track.id == currentId)
                )
            )
        }
        return list
    }

    fun getSubtitleTracksInfo(): List<VlcTrackInfo> {
        val tracks = mediaPlayer.spuTracks ?: return emptyList()
        val currentId = mediaPlayer.spuTrack
        val list = mutableListOf<VlcTrackInfo>()
        for (track in tracks) {
            list.add(
                VlcTrackInfo(
                    id = track.id,
                    name = track.name ?: "Subtitle ${track.id}",
                    isSelected = (track.id == currentId)
                )
            )
        }
        return list
    }

    fun selectAudioTrack(trackId: Int): Boolean {
        return mediaPlayer.setAudioTrack(trackId)
    }

    fun selectSubtitleTrack(trackId: Int): Boolean {
        return mediaPlayer.setSpuTrack(trackId)
    }

    fun disableSubtitles() {
        mediaPlayer.setSpuTrack(-1)
    }

    fun updatePosition() {
        _currentPosition.value = mediaPlayer.time
        _duration.value = mediaPlayer.length.coerceAtLeast(0L)
    }

    fun release() {
        try {
            mediaPlayer.stop()
            mediaPlayer.release()
            libVLC.release()
        } catch (e: Exception) {
            android.util.Log.e("VlcPlayerManager", "Error releasing VLC", e)
        }
    }
}
