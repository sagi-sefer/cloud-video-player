package com.example.cloudvideoplayer.presentation.ui

import android.app.Activity
import android.content.pm.ActivityInfo
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.cloudvideoplayer.presentation.PlayerUiState
import com.example.cloudvideoplayer.presentation.PlayerViewModel
import kotlinx.coroutines.delay
import org.videolan.libvlc.util.VLCVideoLayout

@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    videoId: String,
    videoPath: String,
    videoTitle: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val showResumePrompt by viewModel.showResumePrompt.collectAsState()
    val isPlaying by viewModel.vlcPlayerManager.isPlaying.collectAsState()
    val currentPosition by viewModel.vlcPlayerManager.currentPosition.collectAsState()
    val duration by viewModel.vlcPlayerManager.duration.collectAsState()

    var showControls by remember { mutableStateOf(true) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    // Load video once when videoId changes
    LaunchedEffect(videoId) {
        viewModel.loadVideo(videoId, videoPath, videoTitle)
    }

    // Force Landscape during playback and save progress on dispose
    DisposableEffect(Unit) {
        val activity = context as? Activity
        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        onDispose {
            viewModel.saveProgress()
            activity?.requestedOrientation = originalOrientation
        }
    }

    // 5-second auto-hide controls timer (paused when settings dialog is open)
    LaunchedEffect(showControls, isPlaying, showSettingsDialog) {
        if (showControls && isPlaying && !showSettingsDialog) {
            delay(5000L)
            showControls = false
        }
    }

    // Position updates poller
    LaunchedEffect(isPlaying) {
        while (true) {
            viewModel.vlcPlayerManager.updatePosition()
            delay(1000L)
        }
    }

    // Immersive mode: hide system bars when controls are hidden, show when controls are shown
    DisposableEffect(showControls) {
        val activity = context as? Activity
        if (activity != null) {
            val window = activity.window
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            if (showControls) {
                controller.show(WindowInsetsCompat.Type.systemBars())
            } else {
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
        onDispose {
            val activity = context as? Activity
            if (activity != null) {
                val window = activity.window
                val controller = WindowCompat.getInsetsController(window, window.decorView)
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable {
                if (!showSettingsDialog) {
                    showControls = !showControls
                }
            }
    ) {
        when (val state = uiState) {
            is PlayerUiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            is PlayerUiState.Error -> {
                Text(
                    text = "Error: ${state.message}",
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            is PlayerUiState.Ready -> {
                // VLC Video View Layout
                AndroidView(
                    factory = { ctx ->
                        VLCVideoLayout(ctx).apply {
                            viewModel.vlcPlayerManager.mediaPlayer.attachViews(this, null, false, false)
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    onRelease = { _ ->
                        viewModel.vlcPlayerManager.mediaPlayer.detachViews()
                    }
                )

                // Resume Prompt Dialog
                if (showResumePrompt && state.savedProgress != null) {
                    val posMinutes = state.savedProgress.positionMs / 60000
                    AlertDialog(
                        onDismissRequest = { viewModel.startPlayback(0L) },
                        title = { Text("Resume Playback?") },
                        text = { Text("Would you like to resume playback from ${posMinutes}m?") },
                        confirmButton = {
                            Button(onClick = { viewModel.startPlayback(state.savedProgress.positionMs) }) {
                                Text("Resume")
                            }
                        },
                        dismissButton = {
                            Button(onClick = { viewModel.startPlayback(0L) }) {
                                Text("Start from Beginning")
                            }
                        }
                    )
                }

                // Transparent Overlay Controls with Gradient Scrims
                AnimatedVisibility(
                    visible = showControls,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Top Scrim & Bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)
                                    )
                                )
                                .align(Alignment.TopCenter)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .windowInsetsPadding(WindowInsets.safeDrawing)
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = onBack) {
                                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                                    }
                                    Text(text = state.videoTitle, color = Color.White, style = MaterialTheme.typography.titleMedium)
                                }
                                IconButton(onClick = { showSettingsDialog = true }) {
                                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
                                }
                            }
                        }

                        // Bottom Scrim & Controls Bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                                    )
                                )
                                .align(Alignment.BottomCenter)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .windowInsetsPadding(WindowInsets.safeDrawing)
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                    .align(Alignment.BottomCenter)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = formatTime(currentPosition),
                                        color = Color.White
                                    )
                                    Slider(
                                        value = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f,
                                        onValueChange = { progress ->
                                            val newPos = (progress * duration).toLong()
                                            viewModel.vlcPlayerManager.seekTo(newPos)
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(horizontal = 8.dp)
                                    )
                                    Text(
                                        text = formatTime(duration),
                                        color = Color.White
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = {
                                            if (isPlaying) {
                                                viewModel.vlcPlayerManager.pause()
                                            } else {
                                                viewModel.vlcPlayerManager.resume()
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = if (isPlaying) "Pause" else "Play",
                                            tint = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Audio & Subtitle Settings Dialog (scoped at PlayerScreen top-level)
                if (showSettingsDialog) {
                    val audioTracks = viewModel.vlcPlayerManager.getAudioTracksInfo()
                    val subtitleTracks = viewModel.vlcPlayerManager.getSubtitleTracksInfo()
                    val scrollState = rememberScrollState()

                    AlertDialog(
                        onDismissRequest = { showSettingsDialog = false },
                        title = { Text("Audio & Subtitles") },
                        text = {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(scrollState)
                            ) {
                                Text("Audio Tracks:", style = MaterialTheme.typography.titleSmall)
                                if (audioTracks.isEmpty()) {
                                    Text("No audio tracks available", style = MaterialTheme.typography.bodySmall)
                                } else {
                                    audioTracks.forEach { track ->
                                        Button(
                                            onClick = {
                                                val success = viewModel.vlcPlayerManager.selectAudioTrack(track.id)
                                                if (!success) {
                                                    Toast.makeText(context, "Failed to select audio track '${track.name}'.", Toast.LENGTH_LONG).show()
                                                } else {
                                                    showSettingsDialog = false
                                                }
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 2.dp)
                                        ) {
                                            Text("${if (track.isSelected) "✓ " else ""}${track.name}")
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Subtitle Tracks:", style = MaterialTheme.typography.titleSmall)
                                Button(
                                    onClick = {
                                        viewModel.vlcPlayerManager.disableSubtitles()
                                        showSettingsDialog = false
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp)
                                ) {
                                    Text("Off (Disable Subtitles)")
                                }
                                if (subtitleTracks.isEmpty()) {
                                    Text("No subtitle tracks available", style = MaterialTheme.typography.bodySmall)
                                } else {
                                    subtitleTracks.forEach { track ->
                                        Button(
                                            onClick = {
                                                val success = viewModel.vlcPlayerManager.selectSubtitleTrack(track.id)
                                                if (!success) {
                                                    Toast.makeText(context, "Failed to select subtitle '${track.name}'.", Toast.LENGTH_LONG).show()
                                                } else {
                                                    showSettingsDialog = false
                                                }
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 2.dp)
                                        ) {
                                            Text("${if (track.isSelected) "✓ " else ""}${track.name}")
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            Button(onClick = { showSettingsDialog = false }) {
                                Text("Close")
                            }
                        }
                    )
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
