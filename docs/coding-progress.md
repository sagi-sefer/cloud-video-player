# CloudVideoPlayer Coding Progress & Task Tracker

## Task Checklist

- [x] **Task 1: Domain Models & Dropbox Storage Repository**
  - [x] Create domain models (`MediaItem` sealed class for Folder/VideoItem, `PlaybackProgress`, `VideoFormat`)
  - [x] Define repository interfaces (`CloudStorageRepository`, `VideoPlayerRepository`, `MetadataRepository`)
  - [x] Implement Dropbox Storage Repository (`DropboxStorageRepositoryImpl`) using `DROPBOX_VIDEO_APP_KEY`
- [x] **Task 2: Application Use Cases & Room Database Caching**
  - [x] Implement Room entities, DAOs (`PlaybackProgressDao`, `MetadataDao`), and `CloudVideoDatabase`
  - [x] Implement Use Cases (FetchFolderContentUseCase, GetStreamUrlUseCase, GetPlaybackProgressUseCase, SavePlaybackProgressUseCase)
- [x] **Task 3: Infrastructure Video Player & Subtitle Parser**
  - [x] Implement Media3 ExoPlayer wrapper (`ExoPlayerManager`) with audio and subtitle track selection
  - [x] Implement `.srt` and `.vtt` external subtitle parser / configuration
- [x] **Task 4: Presentation Layer (Jetpack Compose Mobile)**
  - [x] Implement App Navigation & Theme
  - [x] Implement Vertical Detail View & Horizontal Full-Screen Player with 5s auto-hide controls & rotation toggle
  - [x] Implement Resume Prompt Dialog & Subtitle Font Size customization
- [x] **Task 5: Android TV (`androidx.tv`) 10-Foot UI**
  - [x] Implement TV Grid, Focus Management, and Leanback Launcher integration
- [x] **Task 6: Verification, Documentation Upkeep & Polish**
  - [x] Review architecture, update `docs/architecture.md` & `docs/development_notes.md`
  - [x] Verify tracking and completeness
