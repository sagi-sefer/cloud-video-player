# Cloud Video Player - Architecture Documentation

## 1. Overview & Intention
**Cloud Video Player** follows strict **Clean Architecture (Onion Architecture)** and **Separation of Concerns (SoC)**, dividing the codebase into four primary packages under `com.example.cloudvideoplayer`:

```
com.example.cloudvideoplayer/
├── domain/            # Pure Kotlin business models & repository interfaces
├── application/       # Use Cases orchestrating business logic
├── infrastructure/    # Adapters (Dropbox SDK, Media3 ExoPlayer, Room DB, Hilt DI)
└── presentation/      # Jetpack Compose UI (Mobile & TV), ViewModels, Themes
```

---

### Layer Breakdown

#### A. `domain` (Pure Kotlin Layer)
* **Models**:
  * `MediaItem`: Sealed class representing `Folder` and `VideoItem` (supporting MKV, MP4, WEBM, AVI, MOV, M4V).
  * `PlaybackProgress`: Last known playback timestamp persistence model.
* **Interfaces**:
  * `CloudStorageRepository`: Dropbox querying, temporary stream URL resolution, and external subtitle discovery.
  * `VideoPlayerRepository`: Playback progress storage and observation.

#### B. `application` (Use Cases Layer)
* Use cases for fetching folder contents (`FetchFolderContentUseCase`), streaming secure cloud video URLs (`GetStreamUrlUseCase`), managing playback progress (`GetPlaybackProgressUseCase`, `SavePlaybackProgressUseCase`), external subtitles (`GetExternalSubtitlesUseCase`), and video search (`SearchVideosUseCase`).

#### C. `infrastructure` (Adapters & External Drivers Layer)
* **Dropbox Storage Adapter**: Integrates with Dropbox SDK v2 (`DropboxStorageRepositoryImpl`) using `DROPBOX_VIDEO_APP_KEY`.
* **Video Player Adapter**: Wraps AndroidX Media3 `ExoPlayer` (`ExoPlayerManager`) with track selection and external `.srt`/`.vtt` subtitle configuration.
* **Database (Room)**: Caches local playback timestamps (`CloudVideoDatabase`, `PlaybackProgressDao`).
* **Dependency Injection (Hilt)**: Modular bindings (`DropboxModule`, `DatabaseModule`, `PlayerModule`).

#### D. `presentation` (UI & Jetpack Compose Layer)
* **Mobile & TV Themes**: Material 3 dark theme and Jetpack Compose for TV (`androidx.tv`).
* **Views**: Library browser (`LibraryScreen`), vertical detail view with rotation toggle (`VideoDetailScreen`), horizontal full-screen video player with 5-second auto-hide controls (`PlayerScreen`), resume prompt dialog, and Android TV 10-foot UI (`TvLibraryScreen`).
