# Cloud Video Player - Development Notes & Guide

## 1. Dropbox Authentication & App ID Setup
* Configured via `DROPBOX_VIDEO_APP_KEY=...` in `local.properties`, injected at compile time via Gradle `BuildConfig.DROPBOX_VIDEO_APP_KEY`.
* OAuth redirect scheme: `cloudvideoplayer://oauth` with explicit scopes (`files.metadata.read files.content.read`). Handled in `MainActivity`.
* **Important Dropbox App Console Requirement**: In the [Dropbox App Console](https://www.dropbox.com/developers/apps), select your app (ID: `9492785`), go to the **Permissions** tab, and explicitly check/enable **`files.metadata.read`** and **`files.content.read`**, then click **Submit**. Without enabling these permissions in the Dropbox App Console, Dropbox will reject requests with "No scope requested can be granted for this app."
* **Manual Access Token Entry & `local.properties` Auto-Configuration**: `LoginScreen` provides a manual text field for entering or pasting a Dropbox access token directly. Furthermore, you can define `DROPBOX_ACCESS_TOKEN=your_token` in `local.properties`, which gets automatically injected at compile time via `BuildConfig.DROPBOX_ACCESS_TOKEN` and auto-populated into app storage on first launch—ensuring your token persists permanently across builds, uninstalls, and reinstalls without manual entry or browser OAuth.
* **Robust Error Handling**: `LibraryViewModel` restricts automatic logout (`LibraryUiState.Unauthenticated`) strictly to genuine authentication/token errors (`401`, `invalid_access_token`, `expired_access_token`, `DbxAuthException`), preventing transient network errors or timeouts from falsely logging out the user.

## 2. Subtitle Selection & Troubleshooting
* **External Subtitles (`.srt`, `.vtt`)**:
  * Automatically discovered by `GetExternalSubtitlesUseCase` scanning the parent folder of the video in Dropbox.
  * Must be placed in the **exact same Dropbox folder** as the video file.
  * Loaded via temporary secure download links (`getTemporaryLink`).
* **Emulator Network & SSL/TLS Considerations**:
  * If testing on an **Android Emulator**, ensure the emulator has active internet access (test opening browser inside emulator).
  * Ensure the emulator's system date and time are correctly synchronized with host/network; incorrect clock settings cause SSL handshake failures when fetching temporary links from Dropbox (`dl.dropboxusercontent.com`).
* **Embedded Subtitles in MKV/MP4 (VOBSUB, PGS, ASS, SRT)**:
  * Handled natively by LibVLC (`libvlc-all`), which supports all embedded subtitle formats including bitmap/image subtitles (`VOBSUB` / `PGS`) and text subtitles (`ASS`, `SRT`, `VTT`).
* **Track Detection Timing**:
  * Subtitle tracks appear in the Settings dialog once ExoPlayer finishes preparing the media source and firing `onTracksChanged`. If opened immediately upon playback start, allow 1–2 seconds for track discovery.

## 3. Technical Implementation Highlights
* **Domain & Clean Architecture**: Fully structured into `domain`, `application`, `infrastructure`, and `presentation` packages with Hilt dependency injection.
* **Dropbox Integration**: `DropboxStorageRepositoryImpl` queries Dropbox v2 files, folder contents, temporary stream links, and external `.srt`/`.vtt`.
* **Home Folder Selection & Recursive Cataloging**: Allows users to set/save any Dropbox folder as their root video folder via the ⭐ button. Uses `CatalogVideosUseCase` and Dropbox API `withRecursive(true)` to recursively discover all videos across all subfolders and auto-open the cataloged **Video Grid Page** (`LibraryViewMode.GRID`). Includes view mode toggles between Video Grid and Folder Browser.
* **Room Database**: Persists last known playback positions (`PlaybackProgressEntity`, `PlaybackProgressDao`, `CloudVideoDatabase`).
* **LibVLC Video Player Rendering**: Utilizes `VlcPlayerManager` and `VLCVideoLayout` with robust multi-format video streaming and full support for all embedded (VOBSUB, PGS, ASS) and external subtitles.
* **Presentation & Rotation**: Supports vertical detail view with dynamic orientation toggling to landscape full-screen player with 5-second auto-hide controls, gradient scrims, and resume prompt dialog.
* **Android TV**: `TvLibraryScreen` built with Jetpack Compose for TV (`androidx.tv`) featuring D-pad accessible action buttons (**⭐ Set Root**, **🏠 Home**, **⬅️ Back**) and hierarchical folder browsing (Category -> Movie folder -> Video file playback) with Leanback launcher integration.

## 4. Video Image / Thumbnail Discovery (`.jpg`, `.jpeg`)
* **Accompanying Image Files**:
  * Automatically matched with video files based on matching base name (e.g. `movie.mp4` paired with `movie.jpg`) in the same folder or cataloged directory.
  * Resolved via Dropbox temporary secure links (`getTemporaryLink`) and assigned to `VideoItem.thumbnailUrl`.
  * Rendered across mobile and Android TV UI (`LibraryScreen`, `VideoDetailScreen`, `TvLibraryScreen`) using Coil (`AsyncImage`) as posters, grid cards, and banners.
