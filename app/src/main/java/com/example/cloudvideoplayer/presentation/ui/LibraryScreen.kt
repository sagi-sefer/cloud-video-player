package com.example.cloudvideoplayer.presentation.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.cloudvideoplayer.domain.model.MediaItem
import com.example.cloudvideoplayer.presentation.LibraryUiState
import com.example.cloudvideoplayer.presentation.LibraryViewModel
import com.example.cloudvideoplayer.presentation.LibraryViewMode

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
    val context = LocalContext.current
    val currentPath = viewModel.currentPath

    BackHandler {
        viewModel.navigateBack()
    }

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
                        Text(
                            text = if (currentPath.isEmpty()) "Cloud Video Library" else currentPath.substringAfterLast('/')
                        )
                    }
                },
                navigationIcon = {
                    if (!isSearching && currentPath != viewModel.getHomeFolder()) {
                        IconButton(onClick = { viewModel.navigateBack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    if (!isSearching) {
                        IconButton(
                            onClick = {
                                android.util.Log.d("LibraryScreen", "Select Root Folder button clicked")
                                viewModel.openRootFolderDialog()
                            }
                        ) {
                            Icon(Icons.Default.Folder, contentDescription = "Select Root Folder")
                        }
                        IconButton(onClick = { viewModel.toggleViewMode() }) {
                            val state = uiState
                            val isGrid = (state is LibraryUiState.Success && state.viewMode == LibraryViewMode.GRID)
                            Icon(
                                if (isGrid) Icons.AutoMirrored.Filled.List else Icons.Default.GridView,
                                contentDescription = "Toggle View Mode"
                            )
                        }
                        IconButton(onClick = { viewModel.catalogRootFolder(currentPath) }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh Catalog")
                        }
                        IconButton(onClick = { viewModel.goToHomeFolder() }) {
                            Icon(Icons.Default.Home, contentDescription = "Go to Home Folder")
                        }
                    }
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
                    if (state.isCataloging) {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Cataloging videos recursively...")
                        }
                    } else if (state.viewMode == LibraryViewMode.GRID) {
                        if (state.catalogedVideos.isEmpty()) {
                            Text(
                                text = "No videos found in catalog.\nNavigate to a video folder and tap ⭐ to set root & catalog.",
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(24.dp),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 150.dp),
                                contentPadding = PaddingValues(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(state.catalogedVideos) { video ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onVideoClick(video) },
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            if (video.thumbnailUrl != null) {
                                                coil.compose.AsyncImage(
                                                    model = video.thumbnailUrl,
                                                    contentDescription = video.name,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(110.dp),
                                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                                )
                                            } else {
                                                Icon(
                                                    Icons.Default.Movie,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.height(48.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = video.name,
                                                style = MaterialTheme.typography.titleSmall,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "${video.format} • ${video.size / (1024 * 1024)} MB",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
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
                                            val isMovieFolder = viewModel.isAtMovieLevel
                                            ListItem(
                                                headlineContent = { Text(item.name) },
                                                supportingContent = if (isMovieFolder) { { Text("Movie") } } else null,
                                                leadingContent = {
                                                    Icon(
                                                        imageVector = if (isMovieFolder) Icons.Default.Movie else Icons.Default.Folder,
                                                        contentDescription = null,
                                                        tint = if (isMovieFolder) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                    )
                                                },
                                                modifier = Modifier.clickable {
                                                    if (isMovieFolder) {
                                                        viewModel.handleFolderClick(item, onVideoClick)
                                                    } else {
                                                        viewModel.navigateToFolder(item)
                                                    }
                                                }
                                            )
                                        }
                                        is MediaItem.VideoItem -> {
                                            ListItem(
                                                headlineContent = { Text(item.name) },
                                                supportingContent = { Text("Format: ${item.format} • Size: ${item.size / (1024 * 1024)} MB") },
                                                leadingContent = {
                                                    if (item.thumbnailUrl != null) {
                                                        coil.compose.AsyncImage(
                                                            model = item.thumbnailUrl,
                                                            contentDescription = item.name,
                                                            modifier = Modifier.size(56.dp),
                                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                                        )
                                                    } else {
                                                        Icon(Icons.Default.Movie, contentDescription = null)
                                                    }
                                                },
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

    val dialogState by viewModel.rootFolderDialogState.collectAsState()

    if (dialogState.isVisible) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { viewModel.closeRootFolderDialog() },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            title = {
                Column {
                    Text("Select Root Folder", color = MaterialTheme.colorScheme.onSurface)
                    Text("Path: ${if (dialogState.currentPath.isEmpty()) "/" else dialogState.currentPath}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            text = {
                Column(modifier = Modifier.height(350.dp).fillMaxWidth()) {
                    if (dialogState.pathStack.size > 1) {
                        androidx.compose.material3.TextButton(onClick = { viewModel.dialogNavigateBack() }) {
                            Text("⬅️ Back to Parent", color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    if (dialogState.folders.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No folders found.", color = MaterialTheme.colorScheme.onSurface)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium)
                        ) {
                            items(dialogState.folders) { folder ->
                                ListItem(
                                    headlineContent = { Text(folder.name, color = MaterialTheme.colorScheme.onSurface) },
                                    leadingContent = { Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                    colors = androidx.compose.material3.ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    modifier = Modifier.clickable {
                                        android.util.Log.d("LibraryScreen", "Clicked dialog folder: ${folder.name}")
                                        viewModel.navigateDialogFolder(folder)
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                androidx.compose.material3.Button(
                    onClick = {
                        android.util.Log.d("LibraryScreen", "Clicked Save button in root folder dialog")
                        viewModel.saveSelectedRootFolder()
                        Toast.makeText(context, "Root folder saved successfully!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { viewModel.closeRootFolderDialog() }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
                }
            }
        )
    }
}
