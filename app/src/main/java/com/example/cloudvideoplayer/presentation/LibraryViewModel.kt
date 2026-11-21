package com.example.cloudvideoplayer.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cloudvideoplayer.application.FetchFolderContentUseCase
import com.example.cloudvideoplayer.application.SearchVideosUseCase
import com.example.cloudvideoplayer.domain.model.MediaItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface LibraryUiState {
    object Loading : LibraryUiState
    data class Success(val items: List<MediaItem>, val currentPath: String) : LibraryUiState
    data class Error(val message: String) : LibraryUiState
    object Unauthenticated : LibraryUiState
}

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val fetchFolderContentUseCase: FetchFolderContentUseCase,
    private val searchVideosUseCase: SearchVideosUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<LibraryUiState>(LibraryUiState.Loading)
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    private val pathStack = mutableListOf<String>("")

    init {
        loadFolder("")
    }

    fun loadFolder(path: String) {
        viewModelScope.launch {
            _uiState.value = LibraryUiState.Loading
            val result = fetchFolderContentUseCase(path)
            result.fold(
                onSuccess = { items ->
                    _uiState.value = LibraryUiState.Success(items, path)
                },
                onFailure = { error ->
                    if (error is IllegalAccessException) {
                        _uiState.value = LibraryUiState.Unauthenticated
                    } else {
                        _uiState.value = LibraryUiState.Error(error.localizedMessage ?: "Unknown error")
                    }
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
                    _uiState.value = LibraryUiState.Success(videos, "Search: $query")
                },
                onFailure = { error ->
                    _uiState.value = LibraryUiState.Error(error.localizedMessage ?: "Search failed")
                }
            )
        }
    }
}
