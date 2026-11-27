package com.example.cloudvideoplayer

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.cloudvideoplayer.domain.model.MediaItem
import com.example.cloudvideoplayer.presentation.LibraryViewModel
import com.example.cloudvideoplayer.presentation.PlayerViewModel
import com.example.cloudvideoplayer.presentation.theme.CloudVideoPlayerTheme
import com.example.cloudvideoplayer.presentation.ui.LibraryScreen
import com.example.cloudvideoplayer.presentation.ui.LoginScreen
import com.example.cloudvideoplayer.presentation.ui.PlayerScreen
import com.example.cloudvideoplayer.presentation.ui.VideoDetailScreen
import com.example.cloudvideoplayer.presentation.tv.TvLibraryScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.example.cloudvideoplayer.BuildConfig

sealed class Screen {
    object Login : Screen()
    object Library : Screen()
    data class Detail(val videoItem: MediaItem.VideoItem) : Screen()
    data class Player(val videoItem: MediaItem.VideoItem) : Screen()
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
     lateinit var sharedPreferences: SharedPreferences

    private val libraryViewModel: LibraryViewModel by viewModels()
    private val playerViewModel: PlayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleOAuthIntent(intent)

        setContent {
            CloudVideoPlayerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var currentScreen by remember {
                        val token = sharedPreferences.getString("dropbox_access_token", null)
                        val configToken = BuildConfig.DROPBOX_ACCESS_TOKEN
                        val effectiveToken = if (token.isNullOrBlank() && configToken.isNotBlank()) {
                            sharedPreferences.edit().putString("dropbox_access_token", configToken).apply()
                            configToken
                        } else {
                            token
                        }
                        mutableStateOf<Screen>(if (!effectiveToken.isNullOrBlank()) Screen.Library else Screen.Login)
                    }

                    when (val screen = currentScreen) {
                        is Screen.Login -> {
                            LoginScreen(
                                onLoginSuccess = {
                                    currentScreen = Screen.Library
                                    libraryViewModel.loadFolder("")
                                }
                            )
                        }
                        is Screen.Library -> {
                            val isTv = packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_LEANBACK)
                            if (isTv) {
                                TvLibraryScreen(
                                    viewModel = libraryViewModel,
                                    onItemClick = { item ->
                                        when (item) {
                                            is MediaItem.Folder -> libraryViewModel.navigateToFolder(item)
                                            is MediaItem.VideoItem -> currentScreen = Screen.Player(item)
                                        }
                                    },
                                    onLoginRequired = {
                                        sharedPreferences.edit().remove("dropbox_access_token").apply()
                                        currentScreen = Screen.Login
                                    }
                                )
                            } else {
                                LibraryScreen(
                                    viewModel = libraryViewModel,
                                    onFolderClick = { folder ->
                                        libraryViewModel.navigateToFolder(folder)
                                    },
                                    onVideoClick = { video ->
                                        currentScreen = Screen.Detail(video)
                                    },
                                    onLoginRequired = {
                                        sharedPreferences.edit().remove("dropbox_access_token").apply()
                                        currentScreen = Screen.Login
                                    }
                                )
                            }
                        }
                        is Screen.Detail -> {
                            VideoDetailScreen(
                                videoName = screen.videoItem.name,
                                videoPath = screen.videoItem.path,
                                thumbnailUrl = screen.videoItem.thumbnailUrl,
                                onBackClick = {
                                    currentScreen = Screen.Library
                                },
                                onPlayClick = {
                                    currentScreen = Screen.Player(screen.videoItem)
                                }
                            )
                        }
                        is Screen.Player -> {
                            val isTv = packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_LEANBACK)
                            PlayerScreen(
                                viewModel = playerViewModel,
                                videoId = screen.videoItem.id,
                                videoPath = screen.videoItem.path,
                                videoTitle = screen.videoItem.name,
                                onBack = {
                                    currentScreen = if (isTv) Screen.Library else Screen.Detail(screen.videoItem)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleOAuthIntent(intent)
    }

    private fun handleOAuthIntent(intent: Intent) {
        val uri = intent.data
        if (uri != null && uri.scheme == "cloudvideoplayer" && uri.host == "oauth") {
            // Parse token from fragment: cloudvideoplayer://oauth#access_token=...
            val fragment = uri.fragment
            if (fragment != null) {
                val params = fragment.split("&")
                for (param in params) {
                    val kv = param.split("=")
                    if (kv.size == 2 && kv[0] == "access_token") {
                        val token = kv[1]
                        sharedPreferences.edit().putString("dropbox_access_token", token).apply()
                        break
                    }
                }
            }
        }
    }
}
