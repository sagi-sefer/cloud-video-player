# CloudVideo - Technical Specification & Architecture Design

## 1. Overview & Vision
**CloudVideo** is a personal cloud-native video streaming application inspired by and built upon the robust architectural foundations of **CloudAudioPlayer**. It transforms shared cloud storage folders on **Dropbox** (powered by a dedicated new Dropbox App ID) into a high-performance, beautiful video streaming library for movies, series, and home video collections.

CloudVideo is designed for both mobile devices (phones/tablets) and the living room (**Android TV**), featuring advanced container format support (MKV, MP4), multi-language audio/subtitle switching, external subtitle file loading (`.srt`, `.vtt`), subtitle font size customization, vertical/horizontal view toggling, and automatic local playback position resume.

---

## 2. Core Architecture & Design Patterns
CloudVideo strictly adheres to **Clean Architecture (Onion Architecture)** and **Separation of Concerns (SoC)**:
* **`domain`**: Pure Kotlin business models (`MediaItem.Folder`, `VideoItem`, `VideoFormat`, `PlaybackState`) with zero Android dependencies.
* **`application`**: Use Cases orchestrating cloud querying, local Room metadata caching, and playback state management.
* **`infrastructure`**: Adapters for Dropbox REST/SDK API, Media3 ExoPlayer with video rendering & track selection, Room DB local caching, and Hilt DI.
* **`presentation`**: Jetpack Compose (Material 3 for mobile, Jetpack Compose for TV `androidx.tv` for Android TV), ViewModels, and responsive UI layouts.

---

## 3. Key Functional Specifications

### A. Dropbox Integration & Auth
* **Dedicated App ID**: Configured via `DROPBOX_VIDEO_APP_KEY` in `local.properties` injected at compile time via Gradle `BuildConfig`.
* **Dropbox-Focused**: Primary authentication flow is Dropbox OAuth (`cloudvideoplayer://oauth`), optimized for shared family/client cloud folders. Google Drive integration is retained in infrastructure but hidden/optional in the UI.

### B. Supported Video Formats & Hierarchical Browsing
* **Formats**: `.mkv`, `.mp4`, `.webm`, `.avi`, `.mov`, `.m4v`.
* **Hierarchy**: Hierarchical folder navigation (`Director / Movie` or `Series / Season / Episode`), parsing folder trees and sorting items alphabetically.

### C. Advanced MKV & External Subtitle Support
* **Audio Track Switching**: Leverages Media3 `Tracks` and `TrackSelectionParameters` to allow users to switch between available audio languages (e.g., English, Hebrew, Commentary) on the fly.
* **Subtitle Selection**: Supports both embedded MKV/MP4 subtitle streams and **external subtitle files** (`.srt`, `.vtt`) discovered dynamically in the same cloud folder.
* **Subtitle Font Size Settings**: User preference setting (Small, Medium, Large, Extra Large) scaling the ExoPlayer subtitle view rendering.

### D. Player View Modes & Full-Screen Behavior
* **Default Vertical Mode**: Opens in vertical (portrait) view displaying video poster, metadata, description, and chapter/episode list. Includes a YouTube-style rotation/fullscreen button to instantly switch to horizontal (landscape) view.
* **Horizontal Full-Screen Player**:
  * **Immersive Scrims & Controls**: Transparent overlay with subtle dark gradient scrims over the video.
  * **Auto-Hide**: Controls automatically fade out after **5 seconds** of inactivity.
  * **Touch Toggle**: Tapping anywhere on the screen instantly shows or hides the control overlay.
  * **System Bars**: Immersive sticky mode hiding status and navigation bars during playback.

### E. Playback Position Persistence & Resume Prompt
* **Local Persistence**: Saves last known playback timestamps locally via Room database / SharedPreferences.
* **Resume Prompt**: When re-opening a video that was previously watched, the app displays a prompt:
  > *"Resume playback from 01:23:45?"*
  > **[ Resume ]** &nbsp;&nbsp;&nbsp;&nbsp; **[ Start from Beginning ]**

### F. Android TV (10-Foot UI) Support
* **Jetpack Compose for TV (`androidx.tv`)**: Cinematic grids, focus rings, and D-pad navigation optimized for physical TV remotes.
* **TV OSD Controls**: Remote control key mapping (D-pad, Center click, Media keys for Play/Pause/Rewind/Fast-Forward) and leanback launcher integration (`LEANBACK_LAUNCHER`).
