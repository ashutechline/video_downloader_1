package com.example.video_downloder.presentation.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.video_downloder.domain.model.FileType
import com.example.video_downloder.domain.model.StatusItem
import com.example.video_downloder.presentation.viewmodel.StatusViewModel
import android.content.Intent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusPreviewScreen(
    statusId: String,
    onBackClick: () -> Unit,
    viewModel: StatusViewModel
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val status = uiState.allStatuses.find { it.id == statusId }

    if (status == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Status not found")
        }
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Content Area
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (status.fileType == FileType.VIDEO) {
                VideoPlayer(uri = status.uri)
            } else {
                AsyncImage(
                    model = status.uri,
                    contentDescription = status.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
        }

        // Top App Bar Overlay
        TopAppBar(
            title = { Text(status.name, color = Color.White) },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Black.copy(alpha = 0.5f),
                titleContentColor = Color.White
            ),
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // Bottom Bar Overlay
        BottomAppBar(
            containerColor = Color.Black.copy(alpha = 0.5f),
            modifier = Modifier.align(Alignment.BottomCenter)
                .height(140.dp), // Increased height for stacked buttons
            actions = {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            val shareIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_STREAM, status.uri)
                                type = if (status.fileType == FileType.VIDEO) "video/mp4" else "image/jpeg"
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Status"))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Share", color = Color.White)
                    }

                    Button(
                        onClick = { viewModel.saveStatus(status) },
                        enabled = !status.isSaved,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            disabledContainerColor = Color.Gray.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (status.isSaved) "Saved" else "Save", color = Color.White)
                    }
                }
            }
        )
    }
}

@Composable
fun VideoPlayer(uri: Uri) {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    AndroidView(
        factory = {
            PlayerView(context).apply {
                player = exoPlayer
                useController = true
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}
