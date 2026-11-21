package com.example.cloudvideoplayer.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cloudvideoplayer.application.GetExternalSubtitlesUseCase
import com.example.cloudvideoplayer.application.GetPlaybackProgressUseCase
import com.example.cloudvideoplayer.application.GetStreamUrlUseCase
import com.example.cloudvideoplayer.application.SavePlaybackProgressUseCase
import com.example.cloudvideoplayer.domain.model.PlaybackProgress
import com.example.cloudvideoplayer.domain.model.SubtitleItem
import com.example.cloudvideoplayer.infrastructure.player.ExoPlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SubtitleFontSize {
    SMALL, MEDIUM, LARGE, EXTRA_LARGE
}

sealed interface PlayerUiState {
    object Loading : PlayerUiState
    data class Ready(
        val videoTitle: String,
        val streamUrl: String,
        val subtitles: List<SubtitleItem>,
        val savedProgress: PlaybackProgress?
    ) : PlayerUiState
    data class Error(val message: String) : PlayerUiState
}

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val getStreamUrlUseCase: GetStreamUrlUseCase,
    private val getExternalSubtitlesUseCase: GetExternalSubtitlesUseCase,
    private val getPlaybackProgressUseCase: GetPlaybackProgressUseCase,
    private val savePlaybackProgressUseCase: SavePlaybackProgressUseCase,
    val exoPlayerManager: ExoPlayerManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<PlayerUiState>(PlayerUiState.Loading)
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private val _showResumePrompt = MutableStateFlow(false)
    val showResumePrompt: StateFlow<Boolean> = _showResumePrompt.asStateFlow()

    private val _subtitleFontSize = MutableStateFlow(SubtitleFontSize.MEDIUM)
    val subtitleFontSize: StateFlow<SubtitleFontSize> = _subtitleFontSize.asStateFlow()

    private var cachedVideoId: String = ""
    private var cachedStreamUrl: String = ""
    private var cachedSubtitles: List<SubtitleItem> = emptyList()
    private var existingProgress: PlaybackProgress? = null

    fun loadVideo(videoId: String, videoPath: String, videoTitle: String) {
        cachedVideoId = videoId
        viewModelScope.launch {
            _uiState.value = PlayerUiState.Loading

            // 1. Get stream URL
            val urlResult = getStreamUrlUseCase(videoPath)
            if (urlResult.isFailure) {
                _uiState.value = PlayerUiState.Error(urlResult.exceptionOrNull()?.localizedMessage ?: "Failed to get stream URL")
                return@launch
            }
            cachedStreamUrl = urlResult.getOrThrow()

            // 2. Get external subtitles from parent folder
            val folderPath = videoPath.substringBeforeLast('/', "")
            val subsResult = getExternalSubtitlesUseCase(folderPath)
            cachedSubtitles = subsResult.getOrElse { emptyList() }

            // 3. Get saved playback progress
            existingProgress = getPlaybackProgressUseCase(videoId)

            _uiState.value = PlayerUiState.Ready(
                videoTitle = videoTitle,
                streamUrl = cachedStreamUrl,
                subtitles = cachedSubtitles,
                savedProgress = existingProgress
            )

            if (existingProgress != null && existingProgress!!.positionMs > 5000L) {
                _showResumePrompt.value = true
            } else {
                startPlayback(0L)
            }
        }
    }

    fun startPlayback(startPositionMs: Long) {
        _showResumePrompt.value = false
        exoPlayerManager.prepareAndPlay(cachedStreamUrl, cachedSubtitles, startPositionMs)
    }

    fun saveProgress() {
        val position = exoPlayerManager.exoPlayer.currentPosition
        val duration = exoPlayerManager.exoPlayer.duration
        if (cachedVideoId.isNotBlank() && duration > 0) {
            viewModelScope.launch {
                savePlaybackProgressUseCase(
                    PlaybackProgress(
                        videoId = cachedVideoId,
                        positionMs = position,
                        durationMs = duration
                    )
                )
            }
        }
    }

    fun setSubtitleFontSize(size: SubtitleFontSize) {
        _subtitleFontSize.value = size
    }

    override fun onCleared() {
        super.onCleared()
        saveProgress()
    }
}
