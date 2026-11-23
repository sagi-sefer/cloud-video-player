package com.example.cloudvideoplayer.presentation.tv

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.items
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.example.cloudvideoplayer.domain.model.MediaItem
import com.example.cloudvideoplayer.presentation.LibraryUiState
import com.example.cloudvideoplayer.presentation.LibraryViewModel

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvLibraryScreen(
    viewModel: LibraryViewModel,
    onItemClick: (MediaItem) -> Unit,
    onLoginRequired: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val currentPath = viewModel.currentPath

    BackHandler {
        viewModel.navigateBack()
    }

    Surface(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Cloud Video Player - TV Library",
                        style = androidx.tv.material3.MaterialTheme.typography.headlineLarge
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Path: ${if (currentPath.isEmpty()) "/" else currentPath}",
                        style = androidx.tv.material3.MaterialTheme.typography.bodyMedium
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {
                            android.util.Log.d("TvLibraryScreen", "Select Root Folder clicked")
                            viewModel.openRootFolderDialog()
                        }
                    ) {
                        Text("📁 Select Root")
                    }
                    Button(
                        onClick = {
                            viewModel.setAsRootFolder(currentPath)
                            Toast.makeText(context, "Root folder saved: $currentPath", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("💾 Save Root")
                    }
                    if (currentPath != viewModel.getHomeFolder() && currentPath.isNotEmpty()) {
                        Button(
                            onClick = { viewModel.navigateBack() }
                        ) {
                            Text("⬅️ Back")
                        }
                    }
                    Button(
                        onClick = { viewModel.goToHomeFolder() }
                    ) {
                        Text("🏠 Home")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            when (val state = uiState) {
                is LibraryUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Loading library...")
                    }
                }
                is LibraryUiState.Unauthenticated -> {
                    onLoginRequired()
                }
                is LibraryUiState.Success -> {
                    val displayItems = state.items
                    if (displayItems.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No items found in this folder.")
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(4),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(displayItems) { item ->
                                val isMovieFolder = viewModel.isAtMovieLevel
                                androidx.tv.material3.Card(
                                    onClick = {
                                        when (item) {
                                            is MediaItem.Folder -> {
                                                if (isMovieFolder) {
                                                    viewModel.handleFolderClick(item) { video ->
                                                        onItemClick(video)
                                                    }
                                                } else {
                                                    viewModel.navigateToFolder(item)
                                                }
                                            }
                                            is MediaItem.VideoItem -> onItemClick(item)
                                        }
                                    },
                                    modifier = Modifier
                                        .height(180.dp)
                                        .width(260.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = when (item) {
                                                is MediaItem.Folder -> if (isMovieFolder) "🎬 ${item.name}" else "📁 ${item.name}"
                                                is MediaItem.VideoItem -> "🎬 ${item.name}"
                                            },
                                            style = androidx.tv.material3.MaterialTheme.typography.titleMedium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                is LibraryUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Error: ${state.message}")
                    }
                }
            }
        }
    }

    val dialogState by viewModel.rootFolderDialogState.collectAsState()

    if (dialogState.isVisible) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { viewModel.closeRootFolderDialog() },
            containerColor = androidx.compose.ui.graphics.Color(0xFF1E1E24),
            titleContentColor = androidx.compose.ui.graphics.Color.White,
            textContentColor = androidx.compose.ui.graphics.Color.White,
            title = {
                Column {
                    Text("Select Root Folder", color = androidx.compose.ui.graphics.Color.White)
                    Text("Path: ${if (dialogState.currentPath.isEmpty()) "/" else dialogState.currentPath}", style = androidx.tv.material3.MaterialTheme.typography.bodyMedium, color = androidx.compose.ui.graphics.Color.LightGray)
                }
            },
            text = {
                Column(modifier = Modifier.height(350.dp).fillMaxWidth()) {
                    if (dialogState.pathStack.size > 1) {
                        androidx.compose.material3.TextButton(onClick = { viewModel.dialogNavigateBack() }) {
                            Text("⬅️ Back to Parent", color = androidx.compose.ui.graphics.Color(0xFF90CAF9))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    if (dialogState.folders.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No folders found.", color = androidx.compose.ui.graphics.Color.White)
                        }
                    } else {
                        androidx.compose.foundation.lazy.LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(androidx.compose.ui.graphics.Color(0xFF2D2D35), androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                        ) {
                            items(dialogState.folders) { folder ->
                                androidx.compose.material3.ListItem(
                                    headlineContent = { Text(folder.name, color = androidx.compose.ui.graphics.Color.White) },
                                    colors = androidx.compose.material3.ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color(0xFF2D2D35)),
                                    modifier = Modifier.clickable {
                                        android.util.Log.d("TvLibraryScreen", "Clicked dialog folder: ${folder.name}")
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
                        android.util.Log.d("TvLibraryScreen", "Clicked Save button in root folder dialog")
                        viewModel.saveSelectedRootFolder()
                        Toast.makeText(context, "Root folder saved successfully!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Save", color = androidx.compose.ui.graphics.Color.White)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { viewModel.closeRootFolderDialog() }) {
                    Text("Cancel", color = androidx.compose.ui.graphics.Color.White)
                }
            }
        )
    }
}
