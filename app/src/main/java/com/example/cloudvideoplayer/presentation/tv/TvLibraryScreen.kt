package com.example.cloudvideoplayer.presentation.tv

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import com.example.cloudvideoplayer.domain.model.MediaItem
import com.example.cloudvideoplayer.presentation.LibraryUiState
import com.example.cloudvideoplayer.presentation.LibraryViewModel

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvLibraryScreen(
    viewModel: LibraryViewModel,
    onItemClick: (MediaItem) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
        ) {
            androidx.tv.material3.Text(
                text = "Cloud Video Player - TV Library",
                style = androidx.tv.material3.MaterialTheme.typography.headlineLarge
            )
            androidx.tv.material3.Spacer(modifier = Modifier.height(24.dp))

            when (val state = uiState) {
                is LibraryUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        androidx.tv.material3.Text("Loading library...")
                    }
                }
                is LibraryUiState.Success -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(state.items) { item ->
                            androidx.tv.material3.Card(
                                onClick = { onItemClick(item) },
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
                                    androidx.tv.material3.Text(
                                        text = item.name,
                                        style = androidx.tv.material3.MaterialTheme.typography.titleMedium
                                    )
                                }
                            }
                        }
                    }
                }
                else -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        androidx.tv.material3.Text("Please connect Dropbox or check connection.")
                    }
                }
            }
        }
    }
}
