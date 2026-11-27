package com.example.cloudvideoplayer.presentation.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.cloudvideoplayer.BuildConfig

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    val context = LocalContext.current
    val appKey = BuildConfig.DROPBOX_VIDEO_APP_KEY
    var manualToken by remember { mutableStateOf("") }
    val sharedPreferences = remember {
        context.getSharedPreferences("cloud_video_prefs", android.content.Context.MODE_PRIVATE)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Cloud Video Player",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Connect your Dropbox account to stream your movies, series, and video collections in high definition.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = {
                val authUrl = "https://www.dropbox.com/oauth2/authorize?client_id=$appKey&response_type=token&redirect_uri=cloudvideoplayer://oauth&scope=files.metadata.read%20files.content.read"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(authUrl))
                context.startActivity(intent)
            }
        ) {
            Text("Connect Dropbox Account")
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Or enter Dropbox Access Token manually:",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = manualToken,
            onValueChange = { manualToken = it },
            label = { Text("Access Token") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = {
                if (manualToken.isNotBlank()) {
                    sharedPreferences.edit().putString("dropbox_access_token", manualToken.trim()).apply()
                    onLoginSuccess()
                }
            },
            enabled = manualToken.isNotBlank()
        ) {
            Text("Save Token & Login")
        }
    }
}
