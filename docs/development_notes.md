# Cloud Video Player - Development Notes & Guide

## 1. Dropbox Authentication & App ID Setup
* Configured via `DROPBOX_VIDEO_APP_KEY=...` in `local.properties`, injected at compile time via Gradle `BuildConfig.DROPBOX_VIDEO_APP_KEY`.
* OAuth redirect scheme: `cloudvideoplayer://oauth`.

## 2. Technical Implementation Highlights
* **Media3 ExoPlayer Video Rendering**: Utilizes `PlayerView` and `ExoPlayer` with `TrackSelectionParameters` for audio and subtitle track switching.
* **External Subtitles**: Parses `.srt` and `.vtt` sidecar subtitle files from Dropbox folder responses.
* **Local Playback Resume**: Room database tracks playback position per video ID.
* **Orientation Toggle**: Dynamic Activity orientation switching between portrait and landscape modes.
* **Android TV**: Leanback launcher intent and `androidx.tv` focus management.
