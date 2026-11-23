package com.example.cloudvideoplayer.presentation

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cloudvideoplayer.application.CatalogVideosUseCase
import com.example.cloudvideoplayer.application.FetchFolderContentUseCase
import com.example.cloudvideoplayer.application.SearchVideosUseCase
import com.example.cloudvideoplayer.domain.model.MediaItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class LibraryViewMode {
    FOLDER,
    GRID
}

sealed interface LibraryUiState {
    object Loading : LibraryUiState
    data class Success(
        val items: List<MediaItem>,
        val currentPath: String,
        val viewMode: LibraryViewMode = LibraryViewMode.FOLDER,
        val catalogedVideos: List<MediaItem.VideoItem> = emptyList(),
        val isCataloging: Boolean = false
    ) : LibraryUiState
    data class Error(val message: String) : LibraryUiState
    object Unauthenticated : LibraryUiState
}

data class RootFolderDialogState(
    val isVisible: Boolean = false,
    val currentPath: String = "",
    val folders: List<MediaItem.Folder> = emptyList(),
    val pathStack: List<String> = listOf("")
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val fetchFolderContentUseCase: FetchFolderContentUseCase,
    private val catalogVideosUseCase: CatalogVideosUseCase,
    private val searchVideosUseCase: SearchVideosUseCase,
    private val sharedPreferences: SharedPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow<LibraryUiState>(LibraryUiState.Loading)
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    private val _rootFolderDialogState = MutableStateFlow(RootFolderDialogState())
    val rootFolderDialogState: StateFlow<RootFolderDialogState> = _rootFolderDialogState.asStateFlow()

    private val pathStack = mutableListOf<String>()
    private var currentViewMode = LibraryViewMode.FOLDER
    private var catalogedVideosList = listOf<MediaItem.VideoItem>()
    private var isCatalogingInProgress = false

    init {
        android.util.Log.d("LibraryViewModel", "Init started. homeFolder: '${getHomeFolder()}'")
        try {
            val home = getHomeFolder()
            pathStack.add(home)
            loadFolder(home)
            if (home.isNotEmpty()) {
                catalogRootFolder(home)
            }
        } catch (e: Exception) {
            android.util.Log.e("LibraryViewModel", "Exception in LibraryViewModel init", e)
        }
    }

    fun openRootFolderDialog() {
        android.util.Log.d("LibraryViewModel", "openRootFolderDialog clicked")
        try {
            loadDialogFolder("")
        } catch (e: Exception) {
            android.util.Log.e("LibraryViewModel", "Exception in openRootFolderDialog", e)
        }
    }

    fun closeRootFolderDialog() {
        android.util.Log.d("LibraryViewModel", "closeRootFolderDialog clicked")
        try {
            _rootFolderDialogState.value = RootFolderDialogState(isVisible = false)
        } catch (e: Exception) {
            android.util.Log.e("LibraryViewModel", "Exception in closeRootFolderDialog", e)
        }
    }

    fun loadDialogFolder(path: String) {
        viewModelScope.launch {
            android.util.Log.d("LibraryViewModel", "loadDialogFolder started for path: '$path'")
            try {
                val result = fetchFolderContentUseCase(path)
                result.fold(
                    onSuccess = { items ->
                        val folders = items.filterIsInstance<MediaItem.Folder>()
                        android.util.Log.d("LibraryViewModel", "loadDialogFolder success for '$path': found ${folders.size} folders")
                        val currentStack = _rootFolderDialogState.value.pathStack.toMutableList()
                        if (currentStack.lastOrNull() != path) {
                            currentStack.add(path)
                        }
                        _rootFolderDialogState.value = RootFolderDialogState(
                            isVisible = true,
                            currentPath = path,
                            folders = folders,
                            pathStack = currentStack
                        )
                    },
                    onFailure = { error ->
                        android.util.Log.e("LibraryViewModel", "loadDialogFolder failed for path '$path'", error)
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e("LibraryViewModel", "Exception in loadDialogFolder for path '$path'", e)
            }
        }
    }

    fun navigateDialogFolder(folder: MediaItem.Folder) {
        android.util.Log.d("LibraryViewModel", "navigateDialogFolder into '${folder.name}' (path: '${folder.path}')")
        try {
            loadDialogFolder(folder.path)
        } catch (e: Exception) {
            android.util.Log.e("LibraryViewModel", "Exception in navigateDialogFolder", e)
        }
    }

    fun dialogNavigateBack() {
        val stack = _rootFolderDialogState.value.pathStack.toMutableList()
        android.util.Log.d("LibraryViewModel", "dialogNavigateBack requested, stack size: ${stack.size}")
        try {
            if (stack.size > 1) {
                stack.removeAt(stack.size - 1)
                val prevPath = stack.last()
                android.util.Log.d("LibraryViewModel", "dialogNavigateBack navigating to previous path: '$prevPath'")
                viewModelScope.launch {
                    try {
                        val result = fetchFolderContentUseCase(prevPath)
                        result.fold(
                            onSuccess = { items ->
                                val folders = items.filterIsInstance<MediaItem.Folder>()
                                _rootFolderDialogState.value = RootFolderDialogState(
                                    isVisible = true,
                                    currentPath = prevPath,
                                    folders = folders,
                                    pathStack = stack
                                )
                            },
                            onFailure = { error ->
                                android.util.Log.e("LibraryViewModel", "dialogNavigateBack failed for path '$prevPath'", error)
                            }
                        )
                    } catch (e: Exception) {
                        android.util.Log.e("LibraryViewModel", "Exception inside dialogNavigateBack coroutine", e)
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("LibraryViewModel", "Exception in dialogNavigateBack", e)
        }
    }

    fun saveSelectedRootFolder() {
        val selectedPath = _rootFolderDialogState.value.currentPath
        android.util.Log.d("LibraryViewModel", "saveSelectedRootFolder clicked. Selected path: '$selectedPath'")
        try {
            sharedPreferences.edit().putString("dropbox_home_folder", selectedPath).apply()
            pathStack.clear()
            pathStack.add(selectedPath)
            currentViewMode = LibraryViewMode.FOLDER
            loadFolder(selectedPath)
            closeRootFolderDialog()
            android.util.Log.d("LibraryViewModel", "Successfully saved root folder and updated library view to '$selectedPath'")
        } catch (e: Exception) {
            android.util.Log.e("LibraryViewModel", "Exception in saveSelectedRootFolder for path '$selectedPath'", e)
        }
    }

    val currentPath: String
        get() = pathStack.lastOrNull() ?: ""

    val isAtMovieLevel: Boolean
        get() = pathStack.size > 1

    fun handleFolderClick(folder: MediaItem.Folder, onPlayVideo: (MediaItem.VideoItem) -> Unit) {
        viewModelScope.launch {
            if (pathStack.size == 1) {
                navigateToFolder(folder)
            } else {
                _uiState.value = LibraryUiState.Loading
                val result = fetchFolderContentUseCase(folder.path)
                result.fold(
                    onSuccess = { items ->
                        val video = items.filterIsInstance<MediaItem.VideoItem>().firstOrNull()
                        if (video != null) {
                            onPlayVideo(video)
                        } else {
                            navigateToFolder(folder)
                        }
                    },
                    onFailure = { error ->
                        handleError(error)
                    }
                )
            }
        }
    }

    fun loadFolder(path: String) {
        viewModelScope.launch {
            _uiState.value = LibraryUiState.Loading
            val result = fetchFolderContentUseCase(path)
            result.fold(
                onSuccess = { items ->
                    _uiState.value = LibraryUiState.Success(
                        items = items,
                        currentPath = path,
                        viewMode = currentViewMode,
                        catalogedVideos = catalogedVideosList,
                        isCataloging = isCatalogingInProgress
                    )
                },
                onFailure = { error ->
                    handleError(error)
                }
            )
        }
    }

    fun navigateToFolder(folder: MediaItem.Folder) {
        pathStack.add(folder.path)
        loadFolder(folder.path)
    }

    fun navigateBack(): Boolean {
        if (pathStack.size > 1) {
            pathStack.removeAt(pathStack.size - 1)
            loadFolder(pathStack.last())
            return true
        }
        return false
    }

    fun setAsRootFolder(path: String) {
        sharedPreferences.edit().putString("dropbox_home_folder", path).apply()
        pathStack.clear()
        pathStack.add(path)
        currentViewMode = LibraryViewMode.FOLDER
        loadFolder(path)
    }

    fun catalogRootFolder(path: String = currentPath) {
        viewModelScope.launch {
            android.util.Log.d("LibraryViewModel", "catalogRootFolder requested for path: '$path'")
            isCatalogingInProgress = true
            updateSuccessState()
            val result = catalogVideosUseCase(path) { currentVideos ->
                catalogedVideosList = currentVideos
                if (currentVideos.isNotEmpty()) {
                    currentViewMode = LibraryViewMode.GRID
                }
                updateSuccessState()
            }
            isCatalogingInProgress = false
            result.fold(
                onSuccess = { videos ->
                    android.util.Log.d("LibraryViewModel", "catalogRootFolder succeeded with ${videos.size} videos")
                    catalogedVideosList = videos
                    currentViewMode = LibraryViewMode.GRID
                    updateSuccessState()
                },
                onFailure = { error ->
                    android.util.Log.e("LibraryViewModel", "catalogRootFolder failed for '$path'", error)
                    handleError(error)
                }
            )
        }
    }

    fun toggleViewMode() {
        currentViewMode = if (currentViewMode == LibraryViewMode.FOLDER) LibraryViewMode.GRID else LibraryViewMode.FOLDER
        if (currentViewMode == LibraryViewMode.GRID && catalogedVideosList.isEmpty() && !isCatalogingInProgress) {
            catalogRootFolder(currentPath)
        } else {
            updateSuccessState()
        }
    }

    fun getHomeFolder(): String {
        return sharedPreferences.getString("dropbox_home_folder", "") ?: ""
    }

    fun goToHomeFolder() {
        val home = getHomeFolder()
        pathStack.clear()
        pathStack.add(home)
        loadFolder(home)
    }

    private fun updateSuccessState() {
        val currentState = _uiState.value
        if (currentState is LibraryUiState.Success) {
            _uiState.value = currentState.copy(
                viewMode = currentViewMode,
                catalogedVideos = catalogedVideosList,
                isCataloging = isCatalogingInProgress
            )
        } else {
            _uiState.value = LibraryUiState.Success(
                items = emptyList(),
                currentPath = currentPath,
                viewMode = currentViewMode,
                catalogedVideos = catalogedVideosList,
                isCataloging = isCatalogingInProgress
            )
        }
    }

    private fun handleError(error: Throwable) {
        val msg = listOfNotNull(error.message, error.localizedMessage, error.toString()).joinToString(" ")
        if (error is IllegalAccessException || 
            msg.contains("scope", ignoreCase = true) || 
            msg.contains("permitted", ignoreCase = true) || 
            msg.contains("unauthorized", ignoreCase = true) || 
            msg.contains("expired", ignoreCase = true) || 
            msg.contains("token", ignoreCase = true)) {
            _uiState.value = LibraryUiState.Unauthenticated
        } else {
            _uiState.value = LibraryUiState.Error(error.localizedMessage ?: error.message ?: "Unknown error")
        }
    }

    fun search(query: String) {
        if (query.isBlank()) {
            loadFolder(pathStack.lastOrNull() ?: "")
            return
        }
        viewModelScope.launch {
            _uiState.value = LibraryUiState.Loading
            val result = searchVideosUseCase(query)
            result.fold(
                onSuccess = { videos ->
                    _uiState.value = LibraryUiState.Success(
                        items = videos,
                        currentPath = "Search: $query",
                        viewMode = LibraryViewMode.GRID,
                        catalogedVideos = videos,
                        isCataloging = false
                    )
                },
                onFailure = { error ->
                    _uiState.value = LibraryUiState.Error(error.localizedMessage ?: "Search failed")
                }
            )
        }
    }
}
