package com.example.cloudvideoplayer.presentation.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.cloudvideoplayer.domain.model.MediaItem
import com.example.cloudvideoplayer.presentation.LibraryUiState
import com.example.cloudvideoplayer.presentation.LibraryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    onFolderClick: (MediaItem.Folder) -> Unit,
    onVideoClick: (MediaItem.VideoItem) -> Unit,
    onLoginRequired: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearching) {
                        TextField(
                            value = searchQuery,
                            onValueChange = {
                                searchQuery = it
                                viewModel.search(it)
                            },
                            placeholder = { Text("Search videos...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text("Cloud Video Library")
                    }
                },
                actions = {
                    IconButton(onClick = { isSearching = !isSearching }) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = uiState) {
                is LibraryUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is LibraryUiState.Unauthenticated -> {
                    onLoginRequired()
                }
                is LibraryUiState.Error -> {
                    Text(
                        text = "Error: ${state.message}",
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                    )
                }
                is LibraryUiState.Success -> {
                    if (state.items.isEmpty()) {
                        Text(
                            text = "No media found in this folder.",
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(state.items) { item ->
                                when (item) {
                                    is MediaItem.Folder -> {
                                        ListItem(
                                            headlineContent = { Text(item.name) },
                                            leadingContent = { Icon(Icons.Default.Folder, contentDescription = null) },
                                            modifier = Modifier.clickable { onFolderClick(item) }
                                        )
                                    }
                                    is MediaItem.VideoItem -> {
                                        ListItem(
                                            headlineContent = { Text(item.name) },
                                            supportingContent = { Text("Format: ${item.format} • Size: ${item.size / (1024 * 1024)} MB") },
                                            leadingContent = { Icon(Icons.Default.Movie, contentDescription = null) },
                                            modifier = Modifier.clickable { onVideoClick(item) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
