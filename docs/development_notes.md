# Cloud Video Player - Development Notes & Guide

## 1. Dropbox Authentication & App ID Setup
* Configured via `DROPBOX_VIDEO_APP_KEY=...` in `local.properties`, injected at compile time via Gradle `BuildConfig.DROPBOX_VIDEO_APP_KEY`.
* OAuth redirect scheme: `cloudvideoplayer://oauth`. Handled in `MainActivity`.

## 2. Technical Implementation Highlights
* **Domain & Clean Architecture**: Fully structured into `domain`, `application`, `infrastructure`, and `presentation` packages with Hilt dependency injection.
* **Dropbox Integration**: `DropboxStorageRepositoryImpl` queries Dropbox v2 files, folder contents, temporary stream links, and external `.srt`/`.vtt` subtitles.
* **Room Database**: Persists last known playback positions (`PlaybackProgressEntity`, `PlaybackProgressDao`, `CloudVideoDatabase`).
* **Media3 ExoPlayer Video Rendering**: Utilizes `ExoPlayerManager` and `PlayerView` with multi-format video streaming and external subtitle configuration.
* **Presentation & Rotation**: Supports vertical detail view with dynamic orientation toggling to landscape full-screen player with 5-second auto-hide controls, gradient scrims, and resume prompt dialog.
* **Android TV**: `TvLibraryScreen` built with Jetpack Compose for TV (`androidx.tv`) and Leanback launcher integration.
