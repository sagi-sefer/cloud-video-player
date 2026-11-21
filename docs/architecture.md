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
  * `MediaItem`: Sealed class representing `Folder` (Directors, Series, Seasons) and `VideoItem` (supporting MP4, MKV, AVI, MOV, WEBM).
  * `PlaybackProgress`: Last known playback timestamp persistence model.
* **Interfaces**:
  * `CloudStorageRepository`: Dropbox querying, temporary stream URL resolution, and poster art discovery.
  * `VideoPlayerRepository`: Playback controls (`play`, `pause`, `seekTo`, `setAudioTrack`, `setSubtitleTrack`).
  * `MetadataRepository`: Local library caching and retrieval.

#### B. `application` (Use Cases Layer)
* Use cases for fetching folder contents, streaming secure cloud video URLs, syncing library cache, and managing local playback progress resume points.

#### C. `infrastructure` (Adapters & External Drivers Layer)
* **Dropbox Storage Adapter**: Integrates with Dropbox SDK and temporary link resolution.
* **Video Player Adapter**: Wraps AndroidX Media3 `ExoPlayer` with track selector for multi-language audio/subtitles and external `.srt`/`.vtt` subtitle parsing.
* **Database (Room)**: Caches catalog items and local playback timestamps.
* **Dependency Injection (Hilt)**: Modular bindings for storage, player, database, and network.

#### D. `presentation` (UI & Jetpack Compose Layer)
* **Mobile & TV Themes**: Material 3 dark theme and Jetpack Compose for TV (`androidx.tv`).
* **Views**: Vertical detail view, horizontal full-screen video player with 5-second auto-hide controls, subtitle settings, and resume prompt dialogs.
